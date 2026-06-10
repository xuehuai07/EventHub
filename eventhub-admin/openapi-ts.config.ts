import { defineConfig } from '@hey-api/openapi-ts'

export default defineConfig({
  input: 'http://localhost:8080/v3/api-docs',
  output: {
    clean: true,
    path: 'src/shared/api/generated',
  },
  plugins: [
    {
      name: '@hey-api/client-axios',
      baseUrl: false,
    },
  ],
})
