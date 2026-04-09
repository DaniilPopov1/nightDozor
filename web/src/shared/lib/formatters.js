export function formatDateTime(value) {
  if (!value) {
    return 'Не указано'
  }

  return new Date(value).toLocaleString('ru-RU')
}

export function formatTeamRole(role) {
  const roleMap = {
    CAPTAIN: 'Капитан',
    MEMBER: 'Участник',
  }

  return roleMap[role] || role || 'Не указано'
}

export function formatMembershipStatus(status) {
  const statusMap = {
    PENDING: 'Ожидает решения',
    APPROVED: 'Одобрена',
    REJECTED: 'Отклонена',
    ACTIVE: 'Активный участник',
    LEFT: 'Покинул команду',
    REMOVED: 'Исключён',
  }

  return statusMap[status] || status || 'Не указано'
}

export function formatRegistrationStatus(status) {
  const statusMap = {
    PENDING: 'На рассмотрении',
    APPROVED: 'Подтверждена',
    REJECTED: 'Отклонена',
    CANCELED: 'Отменена',
  }

  return statusMap[status] || status || 'Не подана'
}

export function formatGameStatus(status) {
  const statusMap = {
    DRAFT: 'Черновик',
    PUBLISHED: 'Опубликована',
    REGISTRATION_OPEN: 'Регистрация открыта',
    REGISTRATION_CLOSED: 'Регистрация закрыта',
    IN_PROGRESS: 'Игра идёт',
    FINISHED: 'Завершена',
    CANCELLED: 'Отменена',
  }

  return statusMap[status] || status || 'Не указано'
}

export function toDatetimeLocalValue(value) {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  const offset = date.getTimezoneOffset()
  const localDate = new Date(date.getTime() - offset * 60 * 1000)
  return localDate.toISOString().slice(0, 16)
}
