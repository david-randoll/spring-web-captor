import { sendRequest } from '../api/client';

export interface DemoScenario {
  id: string;
  title: string;
  description: string;
  method: string;
  url: string;
  headers: Record<string, string>;
  body: string | null;
  tags: string[];
  run: () => Promise<unknown>;
}

export const DEMOS: DemoScenario[] = [
  {
    id: 'json-post',
    title: 'Create Item (JSON)',
    description: 'POST with a JSON body — see how the library captures the full request and response.',
    method: 'POST',
    url: '/demo/items',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: 'New Gadget', description: 'A cool new gadget', tags: ['demo', 'new'] }, null, 2),
    tags: ['POST', 'JSON', 'Body'],
    run() {
      return sendRequest(this.method, this.url, {
        headers: this.headers,
        body: this.body!,
      });
    },
  },
  {
    id: 'json-get',
    title: 'List Items (GET)',
    description: 'Simple GET request — headers, URL, response body, and timing all captured.',
    method: 'GET',
    url: '/demo/items',
    headers: {},
    body: null,
    tags: ['GET', 'JSON'],
    run() {
      return sendRequest(this.method, this.url);
    },
  },
  {
    id: 'path-vars',
    title: 'Path Variables',
    description: 'Named path params extracted from the URL automatically.',
    method: 'GET',
    url: '/demo/paths/electronics/42',
    headers: {},
    body: null,
    tags: ['GET', 'Path Params'],
    run() {
      return sendRequest(this.method, this.url);
    },
  },
  {
    id: 'wildcard',
    title: 'Catch-All Wildcard',
    description: 'The {*rest} wildcard captures every segment — each split into path1, path2, etc.',
    method: 'GET',
    url: '/demo/paths/wildcard/segment1/segment2/segment3/deep',
    headers: {},
    body: null,
    tags: ['GET', 'Wildcard', 'Path Params'],
    run() {
      return sendRequest(this.method, this.url);
    },
  },
  {
    id: 'query-params',
    title: 'Query Parameters',
    description: 'Multi-value query params — filters, pagination, sorting all captured.',
    method: 'GET',
    url: '/demo/search?q=spring+web+captor&page=1&size=20&sort=relevance&filters=active&filters=premium&filters=verified',
    headers: {},
    body: null,
    tags: ['GET', 'Query Params', 'Multi-value'],
    run() {
      return sendRequest(this.method, this.url);
    },
  },
  {
    id: 'form-data',
    title: 'Form Submission',
    description: 'URL-encoded form body parsed into structured key-value pairs.',
    method: 'POST',
    url: '/demo/form',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'username=john_doe&email=john%40example.com&role=admin',
    tags: ['POST', 'Form'],
    run() {
      return sendRequest(this.method, this.url, {
        headers: this.headers,
        body: this.body!,
      });
    },
  },
  {
    id: 'file-upload',
    title: 'File Upload',
    description: 'Multipart upload — filename, size, content type, and base64 content all captured.',
    method: 'POST',
    url: '/demo/upload',
    headers: {},
    body: '(multipart: demo.txt + description)',
    tags: ['POST', 'Multipart', 'Files'],
    run() {
      const fd = new FormData();
      fd.append('file', new Blob(['Hello from Spring Web Captor!'], { type: 'text/plain' }), 'demo.txt');
      fd.append('description', 'Demo file upload');
      return sendRequest('POST', '/demo/upload', { body: fd });
    },
  },
  {
    id: 'error-500',
    title: 'Server Error (500)',
    description: 'The library captures error details — status, message, and trace.',
    method: 'GET',
    url: '/demo/errors/500',
    headers: {},
    body: null,
    tags: ['GET', '500', 'Error'],
    run() {
      return sendRequest(this.method, this.url).catch(() => {});
    },
  },
  {
    id: 'error-404',
    title: 'Not Found (404)',
    description: 'Missing resources captured with full error detail map.',
    method: 'GET',
    url: '/demo/errors/404',
    headers: {},
    body: null,
    tags: ['GET', '404', 'Error'],
    run() {
      return sendRequest(this.method, this.url).catch(() => {});
    },
  },
  {
    id: 'slow',
    title: 'Slow Request (2s)',
    description: 'Duration extension tracks precise timing — startTime, endTime, and duration.',
    method: 'GET',
    url: '/demo/slow/2',
    headers: {},
    body: null,
    tags: ['GET', 'Duration'],
    run() {
      return sendRequest(this.method, this.url);
    },
  },
  {
    id: 'text-echo',
    title: 'Plain Text Echo',
    description: 'Text body parsing — the library captures non-JSON content types too.',
    method: 'POST',
    url: '/demo/text',
    headers: { 'Content-Type': 'text/plain' },
    body: 'Hello from Spring Web Captor demo!',
    tags: ['POST', 'Text'],
    run() {
      return sendRequest(this.method, this.url, {
        headers: this.headers,
        body: this.body!,
      });
    },
  },
  {
    id: 'validation',
    title: 'Validation Error',
    description: 'Missing required field triggers 400 — error detail shows field-level validation messages.',
    method: 'POST',
    url: '/demo/errors/validation',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ description: 'Missing required name field' }, null, 2),
    tags: ['POST', '400', 'Validation'],
    run() {
      return sendRequest(this.method, this.url, {
        headers: this.headers,
        body: this.body!,
      }).catch(() => {});
    },
  },
];
