export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'OWNER' | 'VIEWER' | 'SERVICE';
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Portfolio {
  id: string;
  userId: string;
  cash: number;
  marketValue: number;
  totalValue: number;
  unrealizedPnl: number;
  realizedPnl: number;
  benchmarkReturnMtd: number;
  benchmarkReturnYtd: number;
  benchmarkReturnSinceInception: number;
  asOfDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface Holding {
  id: string;
  portfolioId: string;
  symbol: string;
  quantity: number;
  costBasis: number;
  costBasisPerShare: number;
  currentPrice: number;
  currentMarketValue: number;
  weightPct: number;
  unrealizedPnl: number;
  realizedPnl: number;
  currency: string;
  sector?: string;
  inUniverse: boolean;
  createdAt: string;
}

export interface Recommendation {
  id: string;
  runId: string;
  symbol: string;
  rank: number;
  targetWeightPct: number;
  currentWeightPct: number;
  weightChangePct: number;
  confidenceScore: number;
  expectedCostBps: number;
  expectedAlphaBps: number;
  edgeOverCostBps: number;
  driver1Name: string;
  driver1Score: number;
  driver2Name: string;
  driver2Score: number;
  driver3Name: string;
  driver3Score: number;
  explanation: string;
  constraintNotes?: string;
  riskContributionPct?: number;
  changeIndicator: string;
  sector: string;
  marketCapTier: string;
  liquidityTier: number;
  currentPrice: number;
  createdAt: string;
}

export interface RecommendationRun {
  id: string;
  userId: string;
  universeId: string;
  constraintSetId: string;
  factorModelVersionId: string;
  scheduledFor: string;
  status: string;
  startedAt?: string;
  completedAt?: string;
  dataFreshnessCheckPassed: boolean;
  dataFreshnessSnapshot?: Record<string, any>;
  constraintSnapshot?: Record<string, any>;
  decisionOutcome?: Record<string, any>;
  runType: 'SCHEDULED' | 'ADHOC';
  numRecommendations: number;
  recommendationCount?: number;
  expectedAlphaBps?: number;
  estimatedCostBps?: number;
  createdAt: string;
  updatedAt: string;
}

export interface Universe {
  id: string;
  name: string;
  description: string;
  tickerList: string[];
  marketCapMin: number;
  marketCapMax: number;
  liquidityTierThresholds: Record<string, any>;
  constituentCount?: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ConstraintSet {
  id: string;
  userId: string;
  name: string;
  maxPositionSizePct: number;
  maxSectorExposurePct: number;
  maxTurnoverPct: number;
  minLiquidityTier: number;
  minMarketCapBn: number;
  cashBufferPct: number;
  taxLossHarvestingEnabled: boolean;
  minHoldingPeriodDays: number;
  maxDrawdownThreshold: number;
  maxNumHoldings: number;
  minNumHoldings: number;
  allowShortSelling: boolean;
  enabledFactors: string[];
  factorWeights: Record<string, number>;
  sectorNeutral: boolean;
  betaNeutral: boolean;
  customConstraints: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

export interface Backtest {
  id: string;
  userId: string;
  name: string;
  description: string;
  universeId: string;
  constraintSetId: string;
  factorModelVersionId: string;
  startDate: string;
  endDate: string;
  initialCapital: number;
  results: Record<string, any>;
  metrics: Record<string, any>;
  status: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  role: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}

export interface Notification {
  id: string;
  userId: string;
  message: string;
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';
  createdAt: string;
  read: boolean;
}

export interface RunStatusUpdate {
  runId: string;
  status: string;
  progress: number;
  stage: string;
  errorMessage?: string;
  timestamp: string;
}
