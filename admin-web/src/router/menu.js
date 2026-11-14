export const recyclerMenu = [
  {
    group: '工作区',
    items: [
      { title: '工作台', path: '/workbench', icon: 'Odometer' },
      { title: '识别用户', path: '/workbench/identify', icon: 'Aim' },
      { title: '投递登记', path: '/workbench/delivery', icon: 'EditPen' },
      { title: '兑换核销', path: '/workbench/redeem', icon: 'Ticket' }
    ]
  },
  {
    group: '查询',
    items: [
      { title: '我的流水', path: '/workbench/records', icon: 'List' },
      { title: '积分规则', path: '/workbench/rules', icon: 'Reading' }
    ]
  },
  {
    group: '账号',
    items: [
      { title: '个人中心', path: '/profile', icon: 'User' },
      { title: '修改密码', path: '/change-password', icon: 'Lock' }
    ]
  }
]

export const adminMenu = [
  {
    group: '概览',
    items: [{ title: '数据看板', path: '/dashboard', icon: 'DataLine' }]
  },
  {
    group: '规则与基础数据',
    items: [
      { title: '垃圾类别', path: '/config/categories', icon: 'Files' },
      { title: '积分规则', path: '/config/rules', icon: 'SetUp' },
      { title: '回收站点', path: '/config/sites', icon: 'Location' }
    ]
  },
  {
    group: '商城',
    items: [{ title: '商品管理', path: '/mall/products', icon: 'Goods' }]
  },
  {
    group: '人员',
    items: [
      { title: '用户账号', path: '/accounts/users', icon: 'User' },
      { title: '回收员审核', path: '/accounts/recyclers', icon: 'UserFilled' }
    ]
  },
  {
    group: '业务审计',
    items: [
      { title: '流水审计', path: '/audit/deliveries', icon: 'Document' },
      { title: '积分调整', path: '/audit/points', icon: 'Coin' },
      { title: '兑换订单', path: '/audit/orders', icon: 'Tickets' },
      { title: '申诉处理', path: '/audit/appeals', icon: 'ChatDotRound' }
    ]
  },
  {
    group: '内容',
    items: [{ title: '公告管理', path: '/content/announcements', icon: 'Bell' }]
  },
  {
    group: '账号',
    items: [
      { title: '个人中心', path: '/profile', icon: 'User' },
      { title: '修改密码', path: '/change-password', icon: 'Lock' }
    ]
  }
]

export function menuForRole(role) {
  return role === 'ADMIN' ? adminMenu : recyclerMenu
}

export function findMenuContext(role, path) {
  const menus = menuForRole(role)
  for (const group of menus) {
    const item = group.items.find((entry) => entry.path === path)
    if (item) return { group: group.group, title: item.title }
  }
  return { group: '', title: '' }
}
