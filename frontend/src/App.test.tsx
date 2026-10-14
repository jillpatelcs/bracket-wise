import '@testing-library/jest-dom/vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import App from './App';

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('calculation requests', () => {
  it('retries a transient gateway error and renders a successful result', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    fetchMock
      .mockResolvedValueOnce({ ok: false, status: 503, json: async () => ({ detail: 'Starting up.' }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ modeledAllocation: { allocations: [] } }) });

    render(<App />);
    fireEvent.click(screen.getByRole('button', { name: 'Run scenario' }));

    expect(await screen.findByRole('status')).toHaveTextContent('may need up to about a minute');
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2), { timeout: 3_000 });
    expect(await screen.findByText('Modeled allocation')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/calculate',
      expect.objectContaining({ method: 'POST' }),
    );
  });

  it('does not retry a validation error', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ detail: 'Only Ontario is supported.' }),
    });
    vi.stubGlobal('fetch', fetchMock);

    render(<App />);
    fireEvent.click(screen.getByRole('button', { name: 'Run scenario' }));

    expect(await screen.findByText('Only Ontario is supported.')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
