import { useEffect, useRef } from 'react'

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)'

export function AuthParticleWave() {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const context = canvas?.getContext('2d')
    if (!canvas || !context) return

    const motionPreference = window.matchMedia(REDUCED_MOTION_QUERY)
    let width = 0
    let height = 0
    let pixelRatio = 1
    let animationFrame = 0
    let startedAt = performance.now()
    let reducedMotion = motionPreference.matches
    let pointerX = 0
    let pointerY = 0
    let targetPointerX = 0
    let targetPointerY = 0

    const resize = () => {
      width = window.innerWidth
      height = window.innerHeight
      pixelRatio = Math.min(window.devicePixelRatio || 1, 2)
      canvas.width = Math.round(width * pixelRatio)
      canvas.height = Math.round(height * pixelRatio)
      canvas.style.width = `${width}px`
      canvas.style.height = `${height}px`
      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
      draw(performance.now())
    }

    const draw = (now: number) => {
      context.clearRect(0, 0, width, height)

      const elapsed = reducedMotion ? 0.8 : (now - startedAt) / 1000
      pointerX += (targetPointerX - pointerX) * 0.14
      pointerY += (targetPointerY - pointerY) * 0.14

      const compact = width < 640
      const columns = compact
        ? 33
        : Math.min(Math.max(Math.round(width / 24), 46), 68)
      const rows = compact ? 26 : 36
      const fieldTop = height * (compact ? 0.03 : 0.015)
      const fieldWidth = width * (compact ? 1.62 : 1.56)
      const fieldDepth = height * (compact ? 1.16 : 1.2)
      const elevation = Math.min(Math.max(height * 0.085, 44), 82)
      const yaw = pointerX * 0.36
      const pitch = pointerY * 0.18
      const cosYaw = Math.cos(yaw)
      const sinYaw = Math.sin(yaw)

      for (let row = 0; row < rows; row += 1) {
        const depth = row / (rows - 1)
        const depthCurve = depth ** 1.5
        const perspective = 0.7 + depthCurve * 0.68
        const worldDepth = (depth - 0.5) * fieldDepth
        const baseY = fieldTop + depthCurve * height * 1.08
        const rowWave = Math.sin(row * 0.52 - elapsed * 1.45)

        for (let column = 0; column < columns; column += 1) {
          const across = column / (columns - 1) - 0.5
          const worldX = across * fieldWidth
          const diagonalWave = Math.sin(
            column * 0.42 + row * 0.22 + elapsed * 1.8,
          )
          const crossWave = Math.sin(
            column * 0.17 - row * 0.48 - elapsed * 1.15,
          )
          const pulse = Math.sin(column * 0.7 + row * 0.83 + elapsed * 2.7)
          const worldZ =
            (diagonalWave * 0.52 +
              rowWave * 0.28 +
              crossWave * 0.2 +
              pulse * 0.1) *
            elevation
          const rotatedX = worldX * cosYaw - worldDepth * sinYaw
          const cameraDepth = worldX * sinYaw + worldDepth * cosYaw
          const lift = worldZ * perspective
          const x =
            width / 2 + rotatedX * perspective + pointerX * (30 + depth * 48)
          const y =
            baseY -
            lift +
            cameraDepth * pitch * 0.24 +
            pointerY * (18 + depth * 30)

          if (x < -18 || x > width + 18 || y < -elevation || y > height + 24) {
            continue
          }

          const accent = (column * 11 + row * 7) % 47
          const normalizedLift = (worldZ / elevation + 1) / 2
          const jumpScale = 0.72 + normalizedLift * 0.72
          const radius =
            (0.44 + depthCurve * (compact ? 2.85 : 3.5)) * jumpScale
          const alpha =
            (0.035 + depthCurve * 0.39) * (0.72 + normalizedLift * 0.44)

          if (normalizedLift > 0.72 && depth > 0.22) {
            context.beginPath()
            context.arc(x, y, radius * 2.8, 0, Math.PI * 2)
            context.fillStyle =
              accent === 0
                ? `rgba(223, 84, 59, ${alpha * 0.09})`
                : accent === 19
                  ? `rgba(211, 166, 49, ${alpha * 0.1})`
                  : `rgba(78, 87, 80, ${alpha * 0.055})`
            context.fill()
          }

          context.beginPath()
          context.arc(x, y, radius, 0, Math.PI * 2)
          context.fillStyle =
            accent === 0
              ? `rgba(223, 84, 59, ${alpha * 0.72})`
              : accent === 19
                ? `rgba(211, 166, 49, ${alpha * 0.82})`
                : `rgba(78, 87, 80, ${alpha})`
          context.fill()
        }
      }
    }

    const animate = (now: number) => {
      draw(now)
      animationFrame = window.requestAnimationFrame(animate)
    }

    const handlePointerMove = (event: PointerEvent) => {
      targetPointerX = event.clientX / Math.max(width, 1) - 0.5
      targetPointerY = event.clientY / Math.max(height, 1) - 0.5
    }

    const handleMotionPreference = (event: MediaQueryListEvent) => {
      reducedMotion = event.matches
      window.cancelAnimationFrame(animationFrame)
      if (reducedMotion) {
        window.removeEventListener('pointermove', handlePointerMove)
        targetPointerX = 0
        targetPointerY = 0
        pointerX = 0
        pointerY = 0
        draw(performance.now())
      } else {
        window.addEventListener('pointermove', handlePointerMove, {
          passive: true,
        })
        startedAt = performance.now()
        animationFrame = window.requestAnimationFrame(animate)
      }
    }

    window.addEventListener('resize', resize)
    if (!reducedMotion) {
      window.addEventListener('pointermove', handlePointerMove, {
        passive: true,
      })
    }
    motionPreference.addEventListener('change', handleMotionPreference)
    resize()
    if (!reducedMotion) {
      animationFrame = window.requestAnimationFrame(animate)
    }

    return () => {
      window.cancelAnimationFrame(animationFrame)
      window.removeEventListener('resize', resize)
      window.removeEventListener('pointermove', handlePointerMove)
      motionPreference.removeEventListener('change', handleMotionPreference)
    }
  }, [])

  return (
    <canvas ref={canvasRef} className="auth-particle-wave" aria-hidden="true" />
  )
}
