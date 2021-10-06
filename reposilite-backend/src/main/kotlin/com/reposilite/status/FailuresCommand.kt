/*
 * Copyright (c) 2021 dzikoysk
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
package com.reposilite.status

import com.reposilite.console.CommandContext
import com.reposilite.console.api.ReposiliteCommand
import picocli.CommandLine.Command

@Command(name = "failures", description = ["Display all recorded exceptions"])
internal class FailuresCommand(private val failureFacade: FailureFacade) : ReposiliteCommand {

    override fun execute(context: CommandContext) {
        if (!failureFacade.hasFailures()) {
            context.append("No exception has occurred yet")
            return
        }

        context.append("")
        context.append("List of cached failures: " + "(" + failureFacade.getFailures().size + ")")
        context.append("")

        failureFacade.getFailures()
            .map { it.split(System.lineSeparator()) }
            .forEach { context.appendAll(it) }
    }

}