export interface SerializedFile {
  filename: string;
  contentType: string;
  size: number;
  base64Content: string;
}

export interface BodyPayload {
  body: unknown;
  files: Record<string, SerializedFile[]>;
}

export interface HttpRequestEvent {
  endpointExists: boolean;
  fullUrl: string;
  path: string;
  method: string;
  headers: Record<string, string[]>;
  queryParams: Record<string, string[]>;
  pathParams: Record<string, string>;
  bodyPayload: BodyPayload | null;
  // Flattened from additionalData via @JsonAnyGetter
  duration?: string;
  startTime?: number;
  endTime?: number;
  userIp?: string;
  userAgent?: string;
  [key: string]: unknown;
}

export interface HttpResponseEvent extends HttpRequestEvent {
  responseBody: unknown;
  responseStatus: string;
  responseHeaders: Record<string, string[]>;
  errorDetail: Record<string, unknown> | null;
}

export interface CapturedEventsResponse {
  requestEvents: HttpRequestEvent[];
  responseEvents: HttpResponseEvent[];
}
