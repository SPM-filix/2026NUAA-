/**
 * Mock 数据仓库
 * 种子数据统一放在仓库根目录 mocks/，与 user-web 共用同一份契约数据。
 * 阶段③接入真实后端后，整个 src/mock 目录不再参与打包。
 */
import categoriesFile from '../../../mocks/categories.json'
import sitesFile from '../../../mocks/sites.json'
import usersFile from '../../../mocks/users.json'
import recyclersFile from '../../../mocks/recyclers.json'
import productsFile from '../../../mocks/products.json'
import deliveriesFile from '../../../mocks/deliveries.json'
import ordersFile from '../../../mocks/orders.json'
import pointsLogsFile from '../../../mocks/points-logs.json'
import announcementsFile from '../../../mocks/announcements.json'
import appealsFile from '../../../mocks/appeals.json'
import rulesFile from '../../../mocks/rules.json'

const clone = (value) => JSON.parse(JSON.stringify(value))

export const db = {
  admins: [
    {
      adminId: 1,
      username: 'admin',
      password: 'admin123',
      mustChangePassword: false,
      createdAt: '2026-08-01T09:00:00+08:00'
    }
  ],
  users: clone(usersFile.data.list),
  recyclers: clone(recyclersFile.data.list),
  sites: clone(sitesFile.data),
  categories: clone(categoriesFile.data),
  products: clone(productsFile.data.list),
  deliveries: clone(deliveriesFile.data.list),
  orders: clone(ordersFile.data.list),
  pointsLogs: clone(pointsLogsFile.data.list),
  announcements: clone(announcementsFile.data.list),
  appeals: clone(appealsFile.data.list),
  rules: clone(rulesFile.data),
  files: []
}

export const DEFAULT_RECYCLER_PASSWORD = 'recycler123'

let seq = 1000
export const nextId = () => ++seq
