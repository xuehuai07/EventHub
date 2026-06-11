import {
  DeleteOutlined,
  InboxOutlined,
  LoadingOutlined,
  PictureOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { Button, Upload, message } from 'antd'
import type { UploadProps } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { uploadCover } from '../../entities/media/api'
import './activity-cover-upload.css'

const { Dragger } = Upload
const MAX_SIZE = 5 * 1024 * 1024
const SUPPORTED_TYPES = ['image/jpeg', 'image/png']

export function ActivityCoverUploader({
  value,
  onChange,
  onUploadingChange,
  disabled,
}: {
  value?: string
  onChange?: (value?: string) => void
  onUploadingChange?: (uploading: boolean) => void
  disabled?: boolean
}) {
  const [uploading, setUploading] = useState(false)
  const [localPreview, setLocalPreview] = useState<string>()
  const [fileName, setFileName] = useState<string>()
  const previewRef = useRef<string | undefined>(undefined)

  useEffect(
    () => () => {
      if (previewRef.current) URL.revokeObjectURL(previewRef.current)
    },
    [],
  )

  const validate = (file: File) => {
    if (!SUPPORTED_TYPES.includes(file.type)) {
      message.error('请选择 JPG 或 PNG 图片')
      return false
    }
    if (file.size > MAX_SIZE) {
      message.error('图片不能超过 5 MB')
      return false
    }
    return true
  }

  const upload: NonNullable<UploadProps['customRequest']> = async ({
    file,
    onSuccess,
    onError,
  }) => {
    const imageFile = file as File
    if (!validate(imageFile)) {
      onError?.(new Error('Invalid image'))
      return
    }
    if (previewRef.current) URL.revokeObjectURL(previewRef.current)
    const preview = URL.createObjectURL(imageFile)
    previewRef.current = preview
    setLocalPreview(preview)
    setFileName(imageFile.name)
    setUploading(true)
    onUploadingChange?.(true)
    try {
      const result = await uploadCover(imageFile)
      if (!result?.url) throw new Error('上传结果缺少图片地址')
      onChange?.(result.url)
      onSuccess?.(result)
      message.success('封面上传成功')
    } catch (error) {
      setLocalPreview(undefined)
      setFileName(undefined)
      onError?.(error as Error)
      message.error(uploadErrorMessage(error))
    } finally {
      setUploading(false)
      onUploadingChange?.(false)
    }
  }

  const clear = () => {
    if (previewRef.current) {
      URL.revokeObjectURL(previewRef.current)
      previewRef.current = undefined
    }
    setLocalPreview(undefined)
    setFileName(undefined)
    onChange?.(undefined)
  }

  const preview = localPreview || value

  if (preview) {
    return (
      <div className="cover-editor">
        <div className="cover-preview-frame">
          <img src={preview} alt="活动封面预览" />
          <div className="cover-preview-shade" />
          <div className="cover-preview-caption">
            <span>{uploading ? '正在上传封面' : '封面已就绪'}</span>
            <strong>{fileName || '活动主视觉'}</strong>
          </div>
          {uploading && (
            <div className="cover-upload-progress">
              <LoadingOutlined />
              正在安全校验并上传
            </div>
          )}
        </div>
        <div className="cover-editor-actions">
          <Dragger
            accept=".jpg,.jpeg,.png,image/jpeg,image/png"
            beforeUpload={validate}
            customRequest={upload}
            disabled={disabled || uploading}
            maxCount={1}
            showUploadList={false}
          >
            <Button icon={<ReloadOutlined />} disabled={disabled || uploading}>
              替换图片
            </Button>
          </Dragger>
          <Button
            danger
            icon={<DeleteOutlined />}
            disabled={disabled || uploading}
            onClick={clear}
          >
            移除封面
          </Button>
          <small>建议比例 16:9，JPG 或 PNG，最大 5 MB</small>
        </div>
      </div>
    )
  }

  return (
    <Dragger
      className="cover-dropzone"
      accept=".jpg,.jpeg,.png,image/jpeg,image/png"
      beforeUpload={validate}
      customRequest={upload}
      disabled={disabled || uploading}
      maxCount={1}
      showUploadList={false}
    >
      <div className="cover-dropzone-art">
        <span className="cover-art-corner">
          <PictureOutlined />
        </span>
        <InboxOutlined className="cover-upload-icon" />
        <strong>选择图片或拖到这里</strong>
        <p>为活动添加一张有辨识度的主视觉</p>
        <span className="cover-file-rules">
          JPG / PNG · 最大 5 MB · 建议 1600 × 900
        </span>
      </div>
    </Dragger>
  )
}

function uploadErrorMessage(error: unknown) {
  const response = (
    error as {
      response?: { data?: { message?: string } }
    }
  ).response
  return response?.data?.message || '封面上传失败，请稍后重试'
}
