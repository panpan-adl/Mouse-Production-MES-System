import http from 'k6/http'
import { check, fail, sleep } from 'k6'
import { Counter } from 'k6/metrics'
import exec from 'k6/execution'

const origin = __ENV.MES_TEST_ORIGIN
if (__ENV.MES_ISOLATED_TEST !== '1' || !/^http:\/\/(127\.0\.0\.1|localhost):\d+$/.test(origin || '')) {
  throw new Error('Provide a loopback MES_TEST_ORIGIN and confirm MES_ISOLATED_TEST=1')
}
const accounts = JSON.parse(__ENV.MES_TEST_ACCOUNTS || '[]')
if (accounts.length < 2 || new Set(accounts.map(a => a.userCode)).size !== accounts.length) {
  throw new Error('Supply at least two distinct test accounts in MES_TEST_ACCOUNTS')
}

const successfulProfiles = new Counter('successful_profiles')
export const options = {
  stages: [
    { duration: '15s', target: 2 },
    { duration: '30s', target: 5 },
    { duration: '30s', target: 10 },
    { duration: '10s', target: 0 },
  ],
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
  thresholds: { checks: ['rate==1'], http_req_failed: ['rate<0.01'] },
}

function payload(response) {
  try { return response.json('data') } catch (_) { return null }
}

export default function () {
  const account = accounts[(exec.vu.idInTest - 1) % accounts.length]
  const login = http.post(origin + '/api/auth/login', JSON.stringify(account), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'password_login' },
  })
  const identity = payload(login)
  if (!check(login, {
    'login returned the expected identity': r =>
      r.status === 200 && identity?.userCode === account.userCode && Boolean(identity.token),
  })) fail('Login failed or returned the wrong identity')

  const auth = { headers: { Authorization: 'Bearer ' + identity.token } }
  try {
    for (let i = 0; i < 3; i++) {
      const profile = http.get(origin + '/api/account/profile', { ...auth, tags: { name: 'account_profile' } })
      if (check(profile, {
        'profile belongs to the same account': r => r.status === 200 && payload(r)?.userCode === account.userCode,
      })) successfulProfiles.add(1)
      sleep(1)
    }
  } finally {
    http.post(origin + '/api/auth/logout', null, { ...auth, tags: { name: 'logout' } })
  }
}
