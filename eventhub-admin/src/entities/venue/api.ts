import {
  create2,
  generateSeats,
  list1,
} from '../../shared/api/generated/sdk.gen'
import type {
  SeatGenerationRequest,
  VenueRequest,
} from '../../shared/api/generated/types.gen'

export async function getVenues() {
  return (await list1({ throwOnError: true })).data.data ?? []
}

export async function createVenue(body: VenueRequest) {
  return (await create2({ body, throwOnError: true })).data.data
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
