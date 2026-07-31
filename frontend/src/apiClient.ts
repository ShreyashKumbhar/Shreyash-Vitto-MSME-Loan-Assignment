import {
  ApiResponseEnvelope,
  BusinessProfileRequest,
  BusinessProfileResponse,
  LoanApplicationRequest,
  LoanApplicationResponse,
  DecisionResponse
} from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

async function request<T>(path: string, options?: RequestInit): Promise<ApiResponseEnvelope<T>> {
  const url = `${BASE_URL}${path}`;
  const headers = {
    'Content-Type': 'application/json',
    ...(options?.headers || {}),
  };

  try {
    const response = await fetch(url, { ...options, headers });
    
    // If we get a 502 Bad Gateway from the Vite proxy, it returns HTML, not JSON
    const contentType = response.headers.get('content-type');
    if (!response.ok && contentType && !contentType.includes('application/json')) {
      throw new Error(`Connection Error (${response.status}): Is the backend running?`);
    }

    // We parse the JSON body as the backend always returns the envelope format
    const body: ApiResponseEnvelope<T> = await response.json();
    return body;
  } catch (error: any) {
    return {
      success: false,
      data: null,
      error: {
        code: 'NETWORK_ERROR',
        message: error.message || 'Unable to connect to the server. Please check your internet connection.',
      }
    };
  }
}

export const apiClient = {
  checkHealth: () => {
    return request<any>('/api/v1/health');
  },

  createBusinessProfile: (profile: BusinessProfileRequest) => {
    return request<BusinessProfileResponse>('/api/v1/business-profiles', {
      method: 'POST',
      body: JSON.stringify(profile),
    });
  },

  createLoanApplication: (businessProfileId: string, application: LoanApplicationRequest) => {
    return request<LoanApplicationResponse>(`/api/v1/business-profiles/${businessProfileId}/applications`, {
      method: 'POST',
      body: JSON.stringify(application),
    });
  },

  requestDecision: (applicationId: string, mode: 'sync' | 'async' = 'sync') => {
    return request<DecisionResponse | { applicationId: string; status: 'PROCESSING' }>(
      `/api/v1/applications/${applicationId}/decision?mode=${mode}`,
      { method: 'POST' }
    );
  },

  getDecision: (applicationId: string) => {
    return request<DecisionResponse | { status: 'PROCESSING' }>(`/api/v1/applications/${applicationId}/decision`, {
      method: 'GET',
    });
  }
};
