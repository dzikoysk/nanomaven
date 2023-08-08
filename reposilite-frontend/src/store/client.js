/*
 * Copyright (c) 2023 dzikoysk
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

import axios from "axios"
import usePlaceholders from "./placeholders"

const { baseUrl } = usePlaceholders()

const createURL = (endpoint) =>
  baseUrl + endpoint

const createClient = (defaultName, defaultSecret) => {
  const defaultAuthorization = () =>
    defaultName && defaultSecret
      ? authorization(defaultName, defaultSecret)
      : {}

  const authorization = (name, secret) => ({
    headers: {
      Authorization: `xBasic ${btoa(`${name}:${secret}`)}`,
    },
  })

  const get = (endpoint, credentials) =>
    axios.get(createURL(endpoint), {
      ...(credentials || defaultAuthorization()),
    })

  return {
    auth: {
      me() {
        return get("/api/auth/me")
      },
    },
    console: {},
    status: {
      instance() {
        return get("/api/status/instance")
      },
      snapshots() {
        return get("/api/status/snapshots")
      }
    },
    statistics: {
      allResolved() {
        return get("/api/statistics/resolved/all")
      }
    },
    maven: {
      content(gav) {
        return get(`/${gav}`)
      },
      details(gav) {
        return get(`/api/maven/details/${gav || ""}`)
      },
      download(gav) {
        return get(`/${gav}`, {
          responseType: "blob",
          ...defaultAuthorization(),
        })
      },
      deploy(gav, file, generateChecksums) {
        return axios.put(createURL(`/${gav}`), file, {
          headers: {
            "Content-Type": "application/octet-stream",
            "X-Generate-Checksums": generateChecksums,
            ...defaultAuthorization().headers,
          }
        })
      },
      generatePom(gav, groupId, artifactId, version) {
        return axios.post(
          createURL(`/api/maven/generate/pom/${gav}/${artifactId}-${version}.pom`),
          {
            groupId,
            artifactId,
            version
          }, {
            headers: {
              "Content-Type": "application/json",
              ...defaultAuthorization().headers,
            }
          }
        )
      },
      delete(gav) {
        return axios.delete(createURL(`/${gav}`), {
          ...defaultAuthorization(),
        })
      }
    },
    settings: {
      domains() {
        return axios.get(createURL("/api/settings/domains"), {
          headers: {
            Accepts: "application/json",
            ...defaultAuthorization().headers,
          },
        })
      },
      schema(name) {
        return axios.get(createURL(`/api/settings/schema/${name}`), {
          headers: {
            Accepts: "application/json",
            ...defaultAuthorization().headers,
          },
        })
      },
      fetch(name) {
        return axios.get(createURL(`/api/settings/domain/${name}`), {
          headers: {
            Accepts: "application/json",
            ...defaultAuthorization().headers,
          },
        })
      },
      update(name, content) {
        return axios.put(createURL(`/api/settings/domain/${name}`), content, {
          headers: {
            "Content-Type": "application/json",
            Accepts: "application/json",
            ...defaultAuthorization().headers,
          },
        })
      },
    },
  }
}

export {
  createURL,
  createClient
}
