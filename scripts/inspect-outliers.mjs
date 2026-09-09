import fs from 'node:fs/promises'

const data = JSON.parse(await fs.readFile('./bgc-aggregated-real-data.json', 'utf8'))
const withDuration = data.allChecks
  .filter((c) => c.completedAt)
  .map((c) => ({ ...c, durationSec: (new Date(c.completedAt) - new Date(c.createdAt)) / 1000 }))
withDuration.sort((a, b) => b.durationSec - a.durationSec)

console.log('상위 5개 최장 완료시간:')
console.log(JSON.stringify(withDuration.slice(0, 5), null, 2))

const pending = data.allChecks.filter((c) => c.status === 'pending')
console.log('\npending 건수:', pending.length)
console.log('pending 예시:', JSON.stringify(pending.slice(0, 3), null, 2))
