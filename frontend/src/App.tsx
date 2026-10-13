import { FormEvent, useMemo, useState } from 'react';

type Mode = 'SINGLE' | 'COUPLE';
type Account = 'TFSA' | 'RRSP' | 'FHSA';

type Person = {
  id: string;
  label: string;
  isNewcomer: boolean;
  arrivalDate: string;
  canadianTaxResidentForFullYear: boolean;
  estimatedEmploymentIncomeFor2026: number;
  availableTfsaRoom: number;
  availableRrspDeductionLimit: number;
  fhsaEligible: boolean;
  availableFhsaParticipationRoom: number;
};

type AllocationInput = {
  person: string;
  account: Account;
  percent: number;
};

type CalculationRequest = {
  mode: Mode;
  province: 'ON';
  taxYear: 2026;
  monthlySavings: number;
  fhsaFirstHomePlan: boolean;
  assumptions: {
    expectedAnnualReturn: number;
    rrspWithdrawalTaxRate: number;
  };
  persons: Person[];
  userAllocation?: AllocationInput[];
};

type CalculationResult = {
  configVersion?: string;
  warnings?: Array<{ code?: string; message?: string }>;
  assumptions?: {
    annualBudget?: string;
    expectedAnnualReturn?: string;
    rrspWithdrawalTaxRate?: string;
  };
  perPersonTax?: Array<{
    person?: string;
    simplifiedEstimatedIncomeTax?: string;
    averageEstimatedTaxRate?: string;
  }>;
  estimatedTaxSavings?: {
    userDefined?: {
      annualTotal?: string;
      cumulativeFiveYears?: string;
    };
    modeled?: {
      annualTotal?: string;
      cumulativeFiveYears?: string;
    };
  };
  userAllocation?: {
    label?: string;
    allocations?: Array<{ person?: string; account?: string; percent?: string | number; amount?: string }>;
    unallocated?: string;
  };
  modeledAllocation?: {
    label?: string;
    allocations?: Array<{ person?: string; account?: string; amount?: string }>;
    unallocated?: string;
  };
  projection?: {
    modeled?: {
      estimatedAfterTaxAccountValue?: string;
      taxSavingsIfSaved?: string;
    };
  };
  explanation?: {
    steps?: Array<{ step?: number; person?: string; account?: string; rule?: string; amount?: string; reason?: string }>; 
    whatDiffers?: string[];
  };
};

const accountSequence: Account[] = ['TFSA', 'RRSP', 'FHSA'];

const makePeople = (mode: Mode): Person[] => {
  if (mode === 'SINGLE') {
    return [
      {
        id: 'A',
        label: 'Person A',
        isNewcomer: true,
        arrivalDate: '2024-09-01',
        canadianTaxResidentForFullYear: true,
        estimatedEmploymentIncomeFor2026: 54000,
        availableTfsaRoom: 6000,
        availableRrspDeductionLimit: 8000,
        fhsaEligible: true,
        availableFhsaParticipationRoom: 3000,
      },
    ];
  }

  return [
    {
      id: 'A',
      label: 'Person A',
      isNewcomer: true,
      arrivalDate: '2024-09-01',
      canadianTaxResidentForFullYear: true,
      estimatedEmploymentIncomeFor2026: 54000,
      availableTfsaRoom: 6000,
      availableRrspDeductionLimit: 8000,
      fhsaEligible: true,
      availableFhsaParticipationRoom: 3000,
    },
    {
      id: 'B',
      label: 'Person B',
      isNewcomer: true,
      arrivalDate: '2024-09-01',
      canadianTaxResidentForFullYear: true,
      estimatedEmploymentIncomeFor2026: 45000,
      availableTfsaRoom: 9000,
      availableRrspDeductionLimit: 5000,
      fhsaEligible: true,
      availableFhsaParticipationRoom: 4000,
    },
  ];
};

const makeDefaultAllocations = (people: Person[]): AllocationInput[] => {
  const map: AllocationInput[] = [];
  for (const person of people) {
    map.push({ person: person.id, account: 'TFSA', percent: person.id === 'A' ? 20 : 30 });
    map.push({ person: person.id, account: 'RRSP', percent: person.id === 'A' ? 20 : 10 });
    map.push({ person: person.id, account: 'FHSA', percent: person.id === 'A' ? 10 : 10 });
  }
  return map;
};

const exampleRequest: CalculationRequest = {
  mode: 'COUPLE',
  province: 'ON',
  taxYear: 2026,
  monthlySavings: 1500,
  fhsaFirstHomePlan: true,
  assumptions: {
    expectedAnnualReturn: 0.05,
    rrspWithdrawalTaxRate: 0.2,
  },
  persons: makePeople('COUPLE'),
  userAllocation: makeDefaultAllocations(makePeople('COUPLE')),
};

const formatCurrency = (value?: string | number): string => {
  if (value === undefined || value === null || value === '') {
    return 'n/a';
  }

  const numeric = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(numeric)) {
    return 'n/a';
  }

  return new Intl.NumberFormat('en-CA', {
    style: 'currency',
    currency: 'CAD',
    minimumFractionDigits: 2,
  }).format(numeric);
};

function App() {
  const [request, setRequest] = useState<CalculationRequest>(exampleRequest);
  const [result, setResult] = useState<CalculationResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'summary' | 'comparison' | 'explanation'>('summary');

  const totalAllocations = useMemo(() => {
    return request.userAllocation?.reduce((sum, item) => sum + item.percent, 0) ?? 0;
  }, [request.userAllocation]);

  const handleModeChange = (mode: Mode) => {
    const people = makePeople(mode);
    setRequest((current) => ({
      ...current,
      mode,
      persons: people,
      userAllocation: makeDefaultAllocations(people),
    }));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/v1/calculate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const payload = await response.json().catch(() => ({ message: 'Calculation failed.' }));
        throw new Error(payload?.detail ?? payload?.message ?? 'Calculation failed.');
      }

      const payload = (await response.json()) as CalculationResult;
      setResult(payload);
    } catch (submitError) {
      const message = submitError instanceof Error ? submitError.message : 'Unexpected error';
      setError(message);
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  const updatePerson = (index: number, field: keyof Person, value: string | number | boolean) => {
    setRequest((current) => ({
      ...current,
      persons: current.persons.map((person, personIndex) =>
        personIndex === index ? { ...person, [field]: value } : person,
      ),
    }));
  };

  const summaryCards = [
    {
      label: 'Annual tax savings',
      value: formatCurrency(result?.estimatedTaxSavings?.userDefined?.annualTotal),
    },
    {
      label: '5-year savings',
      value: formatCurrency(result?.estimatedTaxSavings?.userDefined?.cumulativeFiveYears),
    },
    {
      label: 'Modelled after-tax value',
      value: formatCurrency(result?.projection?.modeled?.estimatedAfterTaxAccountValue),
    },
  ];

  return (
    <main className="page-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Educational planning estimate only</p>
          <h1>BracketWise</h1>
        </div>
        <button type="button" className="secondary" onClick={() => setRequest(exampleRequest)}>
          Try example
        </button>
      </header>

      <form onSubmit={handleSubmit} className="layout">
        <section className="panel">
          <h2>Household</h2>

          <div className="field-row">
            <label>
              Mode
              <select value={request.mode} onChange={(event) => handleModeChange(event.target.value as Mode)}>
                <option value="SINGLE">Single</option>
                <option value="COUPLE">Couple</option>
              </select>
            </label>

            <label>
              Monthly savings
              <input
                type="number"
                step="1"
                value={request.monthlySavings}
                onChange={(event) => setRequest((current) => ({ ...current, monthlySavings: Number(event.target.value) }))}
              />
            </label>
          </div>

          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={request.fhsaFirstHomePlan}
              onChange={(event) => setRequest((current) => ({ ...current, fhsaFirstHomePlan: event.target.checked }))}
            />
            I plan to use my FHSA for a qualifying first home purchase
          </label>

          <div className="field-row compact">
            <label>
              Expected annual return
              <input
                type="number"
                step="0.01"
                value={request.assumptions.expectedAnnualReturn}
                onChange={(event) =>
                  setRequest((current) => ({
                    ...current,
                    assumptions: { ...current.assumptions, expectedAnnualReturn: Number(event.target.value) },
                  }))
                }
              />
            </label>

            <label>
              RRSP withdrawal tax rate
              <input
                type="number"
                step="0.01"
                value={request.assumptions.rrspWithdrawalTaxRate}
                onChange={(event) =>
                  setRequest((current) => ({
                    ...current,
                    assumptions: { ...current.assumptions, rrspWithdrawalTaxRate: Number(event.target.value) },
                  }))
                }
              />
            </label>
          </div>

          <div className="allocation-summary">
            <strong>Allocation total:</strong>
            <span>{totalAllocations.toFixed(2)}%</span>
            <span className={totalAllocations === 100 ? 'ok' : 'warn'}>
              {totalAllocations === 100 ? 'Valid' : 'Adjust to 100%'}
            </span>
          </div>
        </section>

        <section className="panel persons-panel">
          {request.persons.map((person, index) => (
            <div className="person-card" key={person.id}>
              <h3>{person.label}</h3>
              <div className="field-grid">
                <label>
                  Income
                  <input
                    type="number"
                    value={person.estimatedEmploymentIncomeFor2026}
                    onChange={(event) => updatePerson(index, 'estimatedEmploymentIncomeFor2026', Number(event.target.value))}
                  />
                </label>

                <label>
                  TFSA room
                  <input
                    type="number"
                    value={person.availableTfsaRoom}
                    onChange={(event) => updatePerson(index, 'availableTfsaRoom', Number(event.target.value))}
                  />
                </label>

                <label>
                  RRSP room
                  <input
                    type="number"
                    value={person.availableRrspDeductionLimit}
                    onChange={(event) => updatePerson(index, 'availableRrspDeductionLimit', Number(event.target.value))}
                  />
                </label>

                <label>
                  FHSA room
                  <input
                    type="number"
                    value={person.availableFhsaParticipationRoom}
                    onChange={(event) => updatePerson(index, 'availableFhsaParticipationRoom', Number(event.target.value))}
                  />
                </label>

                <label className="checkbox-inline">
                  <input
                    type="checkbox"
                    checked={person.fhsaEligible}
                    onChange={(event) => updatePerson(index, 'fhsaEligible', event.target.checked)}
                  />
                  FHSA eligible
                </label>

                <label className="checkbox-inline">
                  <input
                    type="checkbox"
                    checked={person.canadianTaxResidentForFullYear}
                    onChange={(event) => updatePerson(index, 'canadianTaxResidentForFullYear', event.target.checked)}
                  />
                  Full-year resident
                </label>
              </div>
            </div>
          ))}
        </section>

        <section className="panel">
          <h2>Scenario allocation</h2>
          <div className="allocation-grid">
            {request.userAllocation?.map((row, index) => (
              <div className="allocation-row" key={`${row.person}-${row.account}`}>
                <label>
                  {row.person} / {row.account}
                  <input
                    type="number"
                    step="0.01"
                    value={row.percent}
                    onChange={(event) => {
                      const nextValue = Number(event.target.value);
                      setRequest((current) => ({
                        ...current,
                        userAllocation: (current.userAllocation ?? []).map((item, itemIndex) =>
                          itemIndex === index ? { ...item, percent: nextValue } : item,
                        ),
                      }));
                    }}
                  />
                </label>
              </div>
            ))}
          </div>
        </section>

        <div className="actions">
          <button type="submit" disabled={loading}>{loading ? 'Calculating…' : 'Run scenario'}</button>
        </div>
      </form>

      <section className="panel results">
        <div className="results-header">
          <h2>Results</h2>
          <div className="tab-group">
            <button type="button" className={activeTab === 'summary' ? 'tab active' : 'tab'} onClick={() => setActiveTab('summary')}>
              Summary
            </button>
            <button type="button" className={activeTab === 'comparison' ? 'tab active' : 'tab'} onClick={() => setActiveTab('comparison')}>
              Comparison
            </button>
            <button type="button" className={activeTab === 'explanation' ? 'tab active' : 'tab'} onClick={() => setActiveTab('explanation')}>
              Explanation
            </button>
          </div>
        </div>

        {error ? <div className="message error">{error}</div> : null}

        {!result ? (
          <p className="placeholder">No calculation run yet.</p>
        ) : (
          <>
            {result.warnings?.length ? (
              <div className="message warning">
                {result.warnings.map((warning) => warning.code ?? warning.message ?? 'Warning').join(', ')}
              </div>
            ) : null}

            {activeTab === 'summary' && (
              <>
                <div className="summary-list">
                  {summaryCards.map((card) => (
                    <div className="metric-card" key={card.label}>
                      <dt>{card.label}</dt>
                      <dd>{card.value}</dd>
                    </div>
                  ))}
                </div>

                <div className="summary-grid">
                  <div className="card">
                    <h3>Modeled allocation</h3>
                    <ul>
                      {(result.modeledAllocation?.allocations ?? []).map((line, index) => (
                        <li key={`${line.person}-${line.account}-${index}`}>
                          {line.person} • {line.account}: {formatCurrency(line.amount)}
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div className="card">
                    <h3>Per-person tax</h3>
                    <ul>
                      {(result.perPersonTax ?? []).map((tax) => (
                        <li key={tax.person}>
                          {tax.person}: {formatCurrency(tax.simplifiedEstimatedIncomeTax)}
                          {tax.averageEstimatedTaxRate ? ` (${tax.averageEstimatedTaxRate})` : ''}
                        </li>
                      ))}
                    </ul>
                  </div>

                  <div className="card">
                    <h3>Assumptions</h3>
                    <ul>
                      <li>Expected return: {result.assumptions?.expectedAnnualReturn ?? 'n/a'}</li>
                      <li>RRSP withdrawal tax rate: {result.assumptions?.rrspWithdrawalTaxRate ?? 'n/a'}</li>
                      <li>Annual budget: {formatCurrency(result.assumptions?.annualBudget)}</li>
                    </ul>
                  </div>
                </div>
              </>
            )}

            {activeTab === 'comparison' && (
              <div className="comparison-grid">
                <div className="card">
                  <h3>User-defined</h3>
                  <ul>
                    {(result.userAllocation?.allocations ?? []).map((line, index) => (
                      <li key={`${line.person}-${line.account}-${index}`}>
                        {line.person} • {line.account}: {line.percent ?? 'n/a'}%
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="card">
                  <h3>Modeled</h3>
                  <ul>
                    {(result.modeledAllocation?.allocations ?? []).map((line, index) => (
                      <li key={`${line.person}-${line.account}-${index}`}>
                        {line.person} • {line.account}: {formatCurrency(line.amount)}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            )}

            {activeTab === 'explanation' && (
              <div className="explanation-list">
                {(result.explanation?.steps ?? []).map((step) => (
                  <div key={`${step.step}-${step.person}-${step.account}`} className="explanation-item">
                    <strong>
                      Step {step.step}: {step.person} → {step.account}
                    </strong>
                    <div className="meta">
                      Rule: {step.rule} • Amount: {formatCurrency(step.amount)}
                    </div>
                    <p>{step.reason}</p>
                  </div>
                ))}
                {(result.explanation?.whatDiffers ?? []).map((diff, idx) => (
                  <div className="diff-item" key={`diff-${idx}`}>
                    {diff}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </section>
    </main>
  );
}

export default App;
