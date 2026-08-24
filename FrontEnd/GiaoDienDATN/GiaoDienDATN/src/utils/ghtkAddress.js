const removePrefix = (value, prefixes) => {
  const text = String(value ?? '').trim().replace(/\s+/g, ' ')
  const prefix = prefixes.find(item => text.toLocaleLowerCase().startsWith(item.toLocaleLowerCase()))
  return prefix ? text.slice(prefix.length).trim() : text
}

/** Chuẩn hóa địa chỉ ngay trước khi gửi request tính phí GHTK. */
export function formatGhtkAddress(province, district, ward) {
  const provinceText = String(province ?? '').trim().replace(/\s+/g, ' ')
  const normalizedProvince = /^thừa thiên huế$/i.test(provinceText)
    ? provinceText
    : removePrefix(provinceText, ['Tỉnh ', 'Thành phố '])

  return {
    province: normalizedProvince,
    district: String(district ?? '').trim().replace(/\s+/g, ' '),
    // GHTK fee API nhận ward không kèm tiền tố Phường/Xã/Thị trấn.
    ward: removePrefix(ward, ['Phường ', 'Xã ', 'Thị trấn '])
  }
}
