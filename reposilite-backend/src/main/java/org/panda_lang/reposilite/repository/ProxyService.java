/*
 * Copyright (c) 2020 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.reposilite.repository;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.panda_lang.reposilite.Reposilite;
import org.panda_lang.reposilite.ReposiliteContext;
import org.panda_lang.reposilite.ReposiliteException;
import org.panda_lang.reposilite.error.ErrorDto;
import org.panda_lang.reposilite.error.FailureService;
import org.panda_lang.reposilite.error.ResponseUtils;
import org.panda_lang.reposilite.utils.ArrayUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.function.Option;
import org.panda_lang.utilities.commons.function.Result;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class ProxyService {

    private final boolean storeProxied;
    private final boolean proxyPrivate;
    private final int proxyConnectTimeout;
    private final int proxyReadTimeout;
    private final boolean rewritePathsEnabled;
    private final List<? extends String> proxied;
    private final ExecutorService ioService;
    private final RepositoryService repositoryService;
    private final FailureService failureService;
    private final HttpRequestFactory httpRequestFactory;

    public ProxyService(
            boolean storeProxied,
            boolean proxyPrivate,
            int proxyConnectTimeout,
            int proxyReadTimeout,
            boolean rewritePathsEnabled,
            List<? extends String> proxied,
            ExecutorService ioService,
            FailureService failureService,
            RepositoryService repositoryService) {

        this.storeProxied = storeProxied;
        this.proxyPrivate = proxyPrivate;
        this.proxyConnectTimeout = proxyConnectTimeout;
        this.proxyReadTimeout = proxyReadTimeout;
        this.rewritePathsEnabled = rewritePathsEnabled;
        this.proxied = proxied;
        this.ioService = ioService;
        this.repositoryService = repositoryService;
        this.failureService = failureService;
        this.httpRequestFactory = new NetHttpTransport().createRequestFactory();
    }

    protected Result<CompletableFuture<Result<LookupResponse, ErrorDto>>, ErrorDto> findProxied(ReposiliteContext context) {
        String uri = context.uri();
        Repository repository = repositoryService.getPrimaryRepository();

        // remove repository name if defined
        for (Repository localRepository : repositoryService.getRepositories()) {
            if (uri.startsWith("/" + localRepository.getName())) {
                repository = localRepository;
                uri = uri.substring(1 + localRepository.getName().length());
                break;
            }
        }

        if (!proxyPrivate && repository.isHidden()) {
            return Result.error(new ErrorDto(HttpStatus.SC_NOT_FOUND, "Proxying is disabled in private repositories"));
        }

        // /groupId/artifactId/<content>
        if (StringUtils.countOccurrences(uri, "/") < 3) {
            return Result.error(new ErrorDto(HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION, "Invalid proxied request"));
        }

        String remoteUri = uri;
        CompletableFuture<Result<LookupResponse, ErrorDto>> proxiedTask = new CompletableFuture<>();

        ioService.submit(() -> {
            for (String proxied : proxied) {
                try {
                    HttpRequest remoteRequest = httpRequestFactory.buildGetRequest(new GenericUrl(proxied + remoteUri));
                    remoteRequest.setThrowExceptionOnExecuteError(false);
                    remoteRequest.setConnectTimeout(proxyConnectTimeout * 1000);
                    remoteRequest.setReadTimeout(proxyReadTimeout * 1000);
                    HttpResponse remoteResponse = remoteRequest.execute();

                    if (!remoteResponse.isSuccessStatusCode()) {
                        continue;
                    }

                    HttpHeaders headers = remoteResponse.getHeaders();

                    if ("text/html".equals(headers.getContentType())) {
                        continue;
                    }

                    long contentLength = Option.of(headers.getContentLength()).orElseGet(0L);

                    // Nexus can send misleading for client content-length of chunked responses
                    // ~ https://github.com/dzikoysk/reposilite/issues/549
                    if ("gzip".equals(remoteResponse.getContentEncoding())) {
                        contentLength = 0; // remove content-length header
                    }

                    String[] path = remoteUri.split("/");
                    FileDetailsDto fileDetails = new FileDetailsDto(FileDetailsDto.FILE, ArrayUtils.getLast(path), "", remoteResponse.getContentType(), contentLength);
                    LookupResponse response = new LookupResponse(fileDetails);

                    if (context.method().equals("HEAD")) {
                        return proxiedTask.complete(Result.ok(response));
                    }

                    if (!storeProxied) {
                        context.result(outputStream -> IOUtils.copyLarge(remoteResponse.getContent(), outputStream));
                        return proxiedTask.complete(Result.ok(response));
                    }

                    return store(context, remoteUri, remoteResponse)
                            .onEmpty(() -> proxiedTask.complete(Result.ok(response)))
                            .peek(task -> task.thenAccept(proxiedTask::complete));
                }
                catch (Exception exception) {
                    String message = "Proxied repository " + proxied + " is unavailable due to: " + exception.getMessage();
                    Reposilite.getLogger().error(message);

                    if (!(exception instanceof SocketTimeoutException)) {
                        failureService.throwException(remoteUri, new ReposiliteException(message, exception));
                    }
                }
            }

            return proxiedTask.complete(ResponseUtils.error(HttpStatus.SC_NOT_FOUND, "Artifact not found in local and remote repository"));
        });

        return Result.ok(proxiedTask);
    }

    private Option<CompletableFuture<Result<LookupResponse, ErrorDto>>> store(ReposiliteContext context, String uri, HttpResponse remoteResponse) {
        DiskQuota diskQuota = repositoryService.getDiskQuota();

        if (!diskQuota.hasUsableSpace()) {
            Reposilite.getLogger().warn("Out of disk space - Cannot store proxied artifact " + uri);
            return Option.none();
        }

        String repositoryName = StringUtils.split(uri.substring(1), "/")[0]; // skip first path separator
        Repository repository = repositoryService.getRepository(repositoryName);

        if (repository == null) {
            if (!rewritePathsEnabled) {
                return Option.none();
            }

            uri = repositoryService.getPrimaryRepository().getName() + uri;
        }

        File proxiedFile = repositoryService.getFile(uri);

        return Option.of(repositoryService.storeFile(
                uri,
                proxiedFile,
                remoteResponse::getContent,
                () -> {
                    Reposilite.getLogger().info("Stored proxied " + proxiedFile + " from " + remoteResponse.getRequest().getUrl());
                    context.result(outputStream -> FileUtils.copyFile(proxiedFile, outputStream));
                    return new LookupResponse(FileDetailsDto.of(proxiedFile));
                },
                exception -> new ErrorDto(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Cannot process artifact")));

    }

    public boolean hasProxied() {
        return !proxied.isEmpty();
    }

}
