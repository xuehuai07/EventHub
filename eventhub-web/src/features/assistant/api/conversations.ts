import {
  createAssistantConversation,
  deleteAssistantConversation,
  listAssistantConversations,
  listAssistantMessages,
  renameAssistantConversation,
} from '../../../shared/api/generated/sdk.gen'

export async function getAssistantConversations() {
  return (
    await listAssistantConversations({
      query: { page: 1, pageSize: 50 },
      throwOnError: true,
    })
  ).data.data
}

export async function createConversation(title: string) {
  return (
    await createAssistantConversation({
      body: { title },
      throwOnError: true,
    })
  ).data.data
}

export async function renameConversation(
  conversationId: number,
  title: string,
) {
  return (
    await renameAssistantConversation({
      path: { conversationId },
      body: { title },
      throwOnError: true,
    })
  ).data.data
}

export async function deleteConversation(conversationId: number) {
  await deleteAssistantConversation({
    path: { conversationId },
    throwOnError: true,
  })
}

export async function getAssistantMessages(conversationId: number) {
  return (
    await listAssistantMessages({
      path: { conversationId },
      query: { pageSize: 100 },
      throwOnError: true,
    })
  ).data.data
}
