const BASE_URL = 'http://localhost:11006';

export async function sendRequest(
  method: string,
  path: string,
  options?: {
    headers?: Record<string, string>;
    body?: string | FormData;
  }
): Promise<{ status: number; statusText: string; headers: Record<string, string>; body: string }> {
  const url = `${BASE_URL}${path}`;
  const fetchOptions: RequestInit = {
    method,
    headers: options?.headers,
    body: options?.body,
  };

  // Don't set Content-Type for FormData (browser sets boundary)
  if (options?.body instanceof FormData && options?.headers) {
    delete options.headers['Content-Type'];
    fetchOptions.headers = options.headers;
  }

  const res = await fetch(url, fetchOptions);
  const bodyText = await res.text();

  const resHeaders: Record<string, string> = {};
  res.headers.forEach((value, key) => {
    resHeaders[key] = value;
  });

  return { status: res.status, statusText: res.statusText, headers: resHeaders, body: bodyText };
}

export async function fetchCapturedEvents() {
  const res = await fetch(`${BASE_URL}/api/captured-events`);
  return res.json();
}

export async function clearCapturedEvents() {
  await fetch(`${BASE_URL}/api/captured-events`, { method: 'DELETE' });
}

export async function fetchConfig() {
  const res = await fetch(`${BASE_URL}/api/config`);
  return res.json();
}
