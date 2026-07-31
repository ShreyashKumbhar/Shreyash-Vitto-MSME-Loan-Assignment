export interface BusinessProfileRequest {
  ownerName: string;
  pan: string;
  businessType: 'retail' | 'manufacturing' | 'services';
  monthlyRevenue: number; // Will be formatted properly to handle decimals
}

export interface BusinessProfileResponse {
  businessProfileId: string;
  ownerName: string;
  pan: string;
  businessType: 'retail' | 'manufacturing' | 'services';
  monthlyRevenue: number;
  createdAt: string;
}

export interface LoanApplicationRequest {
  loanAmount: number;
  tenureMonths: number;
  purpose: string;
}

export interface LoanApplicationResponse {
  applicationId: string;
  businessProfileId: string;
  loanAmount: number;
  tenureMonths: number;
  purpose: string;
  status: 'SUBMITTED' | 'DECISIONED';
  createdAt: string;
}

export interface ScoreBreakdown {
  revenueToEmiPoints: number;
  loanToRevenuePoints: number;
  tenurePoints: number;
  businessTypePoints: number;
}

export interface DecisionResponse {
  applicationId: string;
  decision: 'Approved' | 'Rejected';
  creditScore: number;
  reasonCodes: string[];
  scoreBreakdown: ScoreBreakdown;
  decidedAt: string;
}

export interface ApiError {
  code: string;
  message: string;
  details?: { field: string; message: string }[];
}

export interface ApiResponseEnvelope<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
}
