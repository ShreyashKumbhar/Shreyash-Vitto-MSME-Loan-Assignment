import React, { useState } from 'react';
import { apiClient } from './apiClient';
import { DecisionResponse } from './types';

function App() {
  const [formData, setFormData] = useState({
    ownerName: '',
    pan: '',
    businessType: 'services',
    monthlyRevenue: '',
    loanAmount: '',
    tenureMonths: '',
    purpose: ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pollStatus, setPollStatus] = useState<string>('');
  const [decision, setDecision] = useState<DecisionResponse | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);

  const validate = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.ownerName.trim()) newErrors.ownerName = 'Owner name is required';
    if (!formData.pan.match(/^[A-Z]{5}[0-9]{4}[A-Z]{1}$/)) newErrors.pan = 'Invalid PAN format (e.g. ABCDE1234F)';
    if (!formData.businessType) newErrors.businessType = 'Business type is required';
    if (!formData.monthlyRevenue || Number(formData.monthlyRevenue) <= 0) newErrors.monthlyRevenue = 'Revenue must be > 0';
    if (!formData.loanAmount || Number(formData.loanAmount) <= 0) newErrors.loanAmount = 'Loan amount must be > 0';
    if (!formData.tenureMonths || Number(formData.tenureMonths) <= 0) newErrors.tenureMonths = 'Tenure must be > 0';
    if (!formData.purpose.trim()) newErrors.purpose = 'Purpose is required';
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handlePoll = async (applicationId: string) => {
    try {
      const res = await apiClient.getDecision(applicationId);
      if (res.success && res.data) {
        if ('status' in res.data && res.data.status === 'PROCESSING') {
          setTimeout(() => handlePoll(applicationId), 2000);
        } else {
          setDecision(res.data as DecisionResponse);
          setIsSubmitting(false);
          setPollStatus('');
        }
      } else {
        setApiError(res.error?.message || 'Failed to poll decision');
        setIsSubmitting(false);
        setPollStatus('');
      }
    } catch (e) {
      setApiError('Network error during polling');
      setIsSubmitting(false);
      setPollStatus('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    
    setIsSubmitting(true);
    setApiError(null);
    setPollStatus('Creating Profile...');

    try {
      // 1. Create Business Profile
      const profileRes = await apiClient.createBusinessProfile({
        ownerName: formData.ownerName,
        pan: formData.pan,
        businessType: formData.businessType as 'retail' | 'manufacturing' | 'services',
        monthlyRevenue: Number(formData.monthlyRevenue)
      });

      if (!profileRes.success || !profileRes.data) {
        throw new Error(profileRes.error?.message || 'Failed to create profile');
      }

      setPollStatus('Submitting Application...');
      
      // 2. Create Application
      const appRes = await apiClient.createLoanApplication(profileRes.data.businessProfileId, {
        loanAmount: Number(formData.loanAmount),
        tenureMonths: Number(formData.tenureMonths),
        purpose: formData.purpose
      });

      if (!appRes.success || !appRes.data) {
        throw new Error(appRes.error?.message || 'Failed to create application');
      }

      setPollStatus('Computing Decision...');

      // 3. Request Decision (Async Mode)
      const decRes = await apiClient.requestDecision(appRes.data.applicationId, 'async');
      
      if (!decRes.success || !decRes.data) {
        throw new Error(decRes.error?.message || 'Failed to request decision');
      }

      if ('status' in decRes.data && decRes.data.status === 'PROCESSING') {
        setPollStatus('Processing Evaluation... Please wait.');
        handlePoll(appRes.data.applicationId);
      } else {
        setDecision(decRes.data as DecisionResponse);
        setIsSubmitting(false);
        setPollStatus('');
      }

    } catch (err: any) {
      setApiError(err.message);
      setIsSubmitting(false);
      setPollStatus('');
    }
  };

  const resetForm = () => {
    setDecision(null);
    setFormData({
      ownerName: '', pan: '', businessType: 'services', monthlyRevenue: '',
      loanAmount: '', tenureMonths: '', purpose: ''
    });
  };

  if (decision) {
    const isApproved = decision.decision === 'Approved';
    return (
      <div className="container">
        <div className={`card-glass result-card ${isApproved ? 'approved' : 'rejected'}`}>
          <div className="result-header">
            <h2>Decision: <span className={isApproved ? 'text-approved' : 'text-rejected'}>{decision.decision}</span></h2>
            <div className="score-circle">
              <span className="score-value">{decision.creditScore}</span>
              <span className="score-label">/ 100</span>
            </div>
          </div>
          
          <div className="reason-codes">
            <h3>Reasoning</h3>
            <ul>
              {decision.reasonCodes.map((code, idx) => (
                <li key={idx} className="reason-badge">{code.replace(/_/g, ' ')}</li>
              ))}
            </ul>
          </div>
          
          <div className="breakdown">
            <h3>Score Breakdown</h3>
            <div className="breakdown-grid">
              <div>Revenue Ratio: {decision.scoreBreakdown.revenueToEmiPoints} pts</div>
              <div>Loan Size: {decision.scoreBreakdown.loanToRevenuePoints} pts</div>
              <div>Tenure Risk: {decision.scoreBreakdown.tenurePoints} pts</div>
              <div>Sector Risk: {decision.scoreBreakdown.businessTypePoints} pts</div>
            </div>
          </div>

          <button className="btn-primary mt-4" onClick={resetForm}>Start New Application</button>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <div className="card-glass form-card animate-fade-in">
        <h1 className="title">MSME Lending Decision System</h1>
        <p className="subtitle">Submit business profile and loan requirements for instant credit evaluation.</p>
        
        {apiError && <div className="error-banner">{apiError}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-section">
            <h3>Business Profile</h3>
            <div className="form-grid">
              <div className="input-group">
                <label>Owner Name</label>
                <input type="text" name="ownerName" value={formData.ownerName} onChange={handleChange} disabled={isSubmitting} />
                {errors.ownerName && <span className="error-text">{errors.ownerName}</span>}
              </div>
              <div className="input-group">
                <label>PAN Number</label>
                <input type="text" name="pan" placeholder="ABCDE1234F" value={formData.pan} onChange={handleChange} disabled={isSubmitting} />
                {errors.pan && <span className="error-text">{errors.pan}</span>}
              </div>
              <div className="input-group">
                <label>Business Type</label>
                <select name="businessType" value={formData.businessType} onChange={handleChange} disabled={isSubmitting}>
                  <option value="services">Services</option>
                  <option value="retail">Retail</option>
                  <option value="manufacturing">Manufacturing</option>
                </select>
                {errors.businessType && <span className="error-text">{errors.businessType}</span>}
              </div>
              <div className="input-group">
                <label>Monthly Revenue (₹)</label>
                <input type="number" name="monthlyRevenue" value={formData.monthlyRevenue} onChange={handleChange} disabled={isSubmitting} />
                {errors.monthlyRevenue && <span className="error-text">{errors.monthlyRevenue}</span>}
              </div>
            </div>
          </div>

          <div className="form-section">
            <h3>Loan Details</h3>
            <div className="form-grid">
              <div className="input-group">
                <label>Loan Amount (₹)</label>
                <input type="number" name="loanAmount" value={formData.loanAmount} onChange={handleChange} disabled={isSubmitting} />
                {errors.loanAmount && <span className="error-text">{errors.loanAmount}</span>}
              </div>
              <div className="input-group">
                <label>Tenure (Months)</label>
                <input type="number" name="tenureMonths" value={formData.tenureMonths} onChange={handleChange} disabled={isSubmitting} />
                {errors.tenureMonths && <span className="error-text">{errors.tenureMonths}</span>}
              </div>
              <div className="input-group full-width">
                <label>Purpose</label>
                <input type="text" name="purpose" placeholder="e.g. Working Capital, Expansion" value={formData.purpose} onChange={handleChange} disabled={isSubmitting} />
                {errors.purpose && <span className="error-text">{errors.purpose}</span>}
              </div>
            </div>
          </div>

          <div className="submit-actions">
            <button type="submit" className="btn-primary" disabled={isSubmitting}>
              {isSubmitting ? (pollStatus || 'Processing...') : 'Submit Application'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default App;
