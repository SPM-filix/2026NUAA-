import { http } from './request'

export function uploadFile(file, scene = 'PRODUCT') {
  const form = new FormData()
  form.append('file', file)
  form.append('scene', scene)
  return http.post('/files', form)
}
