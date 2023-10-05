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

import { computed, ref, watchEffect } from "vue"
import { createClient } from './client'

const token = ref({
  name: localStorage.getItem('token-name') || '',
  secret: localStorage.getItem('token-secret') || '',
})
const setToken = (name, secret) => {
  token.value = { name, secret }
}

watchEffect(() => {
  localStorage.setItem('token-name', token.value.name)
  localStorage.setItem('token-secret', token.value.secret)
})

const details = ref()

const logout = () => {
  setToken('', '')
  details.value = undefined
}

const login = (name, secret) =>
  createClient(name, secret)
    .auth
    .me()
    .then(response => {
      setToken(name, secret)
      details.value = response.data
    })

const client = computed(() => createClient(token.value.name, token.value.secret))
const initializeSession = () => login(token.value.name, token.value.secret)
const isLogged = computed(() => details.value !== undefined)

const isManager = computed(() => {
  return details.value
    ?.permissions
    ?.some(entry => entry.identifier === 'access-token:manager') == true
})

const hasPermissionTo = (path, permission) => {
  if (isManager.value) {
    return true
  }
  return details.value
    ?.routes
    ?.find(route => path.startsWith(route.path) && route.permission.identifier === permission) !== undefined
}

export function useSession() {
  return {
    token,
    details,
    login,
    logout,
    isLogged,
    client,
    isManager,
    hasPermissionTo,
    initializeSession
  }
}