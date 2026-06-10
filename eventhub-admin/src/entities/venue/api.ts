import { create, generateSeats, list } from '../../shared/api/generated/sdk.gen'
import type {
  SeatGenerationRequest,
  VenueRequest,
} from '../../shared/api/generated/types.gen'

export async function getVenues() {
  return (await list({ throwOnError: true })).data.data ?? []
}

export async function createVenue(body: VenueRequest) {
  return (await create({ body, throwOnError: true })).data.data
}

export async function configureVenueSeats(
  venueId: number,
  body: SeatGenerationRequest,
) {
  return (
    await generateSeats({
      path: { venueId },
      body,
      throwOnError: true,
    })
  ).data.data
}
