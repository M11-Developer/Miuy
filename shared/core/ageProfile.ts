export type AgeBand = '3-5' | '6-8' | '9-12' | '13-17' | '18+';

export interface AgeProfileConfig {
  band: AgeBand;
  minAge: number;
  maxAge: number;
  labelAr: string;
  labelEn: string;
  vocabulary: 'simple' | 'medium' | 'teen' | 'adult';
  gameComplexity: 'very-simple' | 'simple' | 'medium' | 'teen' | 'adult';
  sessionMinutes: number;
  filterStrength: 'strict' | 'medium' | 'light';
  needsGuardian: boolean;
  voiceStyle: 'child-friendly' | 'teen' | 'adult';
}

export const AGE_PROFILES: AgeProfileConfig[] = [
  { band: '3-5', minAge: 3, maxAge: 5, labelAr: '٣-٥ سنوات · لعب آمن', labelEn: '3-5 years · gentle play', vocabulary: 'simple', gameComplexity: 'very-simple', sessionMinutes: 10, filterStrength: 'strict', needsGuardian: true, voiceStyle: 'child-friendly' },
  { band: '6-8', minAge: 6, maxAge: 8, labelAr: '٦-٨ سنوات · لعب آمن', labelEn: '6-8 years · gentle play', vocabulary: 'simple', gameComplexity: 'simple', sessionMinutes: 15, filterStrength: 'strict', needsGuardian: true, voiceStyle: 'child-friendly' },
  { band: '9-12', minAge: 9, maxAge: 12, labelAr: '٩-١٢ سنة · لعب آمن', labelEn: '9-12 years · safe play', vocabulary: 'medium', gameComplexity: 'medium', sessionMinutes: 25, filterStrength: 'medium', needsGuardian: true, voiceStyle: 'child-friendly' },
  { band: '13-17', minAge: 13, maxAge: 17, labelAr: '١٣-١٧ سنة · وضع آمن', labelEn: '13-17 years · safe mode', vocabulary: 'teen', gameComplexity: 'teen', sessionMinutes: 50, filterStrength: 'medium', needsGuardian: false, voiceStyle: 'teen' },
  { band: '18+', minAge: 18, maxAge: 99, labelAr: '١٨+ · وضع آمن', labelEn: '18+ · safe mode', vocabulary: 'adult', gameComplexity: 'adult', sessionMinutes: 60, filterStrength: 'light', needsGuardian: false, voiceStyle: 'adult' },
];

export function getProfileForAge(age: number | 'adult'): AgeProfileConfig {
  if (age === 'adult') return AGE_PROFILES[4];
  const n = typeof age === 'number' ? age : 8;
  return AGE_PROFILES.find(p => n >= p.minAge && n <= p.maxAge) || AGE_PROFILES[2];
}

export function getSafetyRules() {
  return {
    noSexualContentForMinors: true,
    noSelfHarmEncouragement: true,
    noDangerousInstructions: true,
    noIllegalGuidance: true,
    noSurveillance: true,
    noHumanImpersonation: true,
    profanityFilter: true,
    respectMeter: true,
    moodSystem: true,
  };
}
