/**
 * Shared Play Lab core - used by Desktop, Android, iOS
 * Understands Arabic and English toy commands
 */

export type ToyKey = 'car' | 'ball' | 'teddy' | 'book' | 'rocket' | 'flower' | 'puzzle' | 'balloon' | 'musical';

export interface ToyDefinition {
  key: ToyKey;
  labelEn: string;
  labelAr: string;
  emoji: string;
  keywords: string[]; // lowercased
  animation: 'float' | 'bounce' | 'wiggle' | 'spin';
  sound?: string;
}

export const TOYS: ToyDefinition[] = [
  { key: 'car', labelEn: 'Toy car', labelAr: 'عربية لعبة', emoji: '🚗', keywords: ['car','toy car','سيارة','عربية','عربيه','سياره','ماشين'], animation: 'bounce' },
  { key: 'ball', labelEn: 'Ball', labelAr: 'كرة', emoji: '⚽', keywords: ['ball','كرة','كوره','كورة'], animation: 'bounce' },
  { key: 'teddy', labelEn: 'Teddy bear', labelAr: 'دبدوب', emoji: '🧸', keywords: ['teddy','bear','دبدوب','دب','تيدي'], animation: 'wiggle' },
  { key: 'book', labelEn: 'Story book', labelAr: 'كتاب', emoji: '📚', keywords: ['book','story','كتاب','قصه','قصة'], animation: 'float' },
  { key: 'rocket', labelEn: 'Rocket', labelAr: 'صاروخ', emoji: '🚀', keywords: ['rocket','صاروخ'], animation: 'float' },
  { key: 'flower', labelEn: 'Flower', labelAr: 'وردة', emoji: '🌸', keywords: ['flower','وردة','زهرة','ورد'], animation: 'wiggle' },
  { key: 'puzzle', labelEn: 'Puzzle', labelAr: 'بازل', emoji: '🧩', keywords: ['puzzle','بازل','لغز'], animation: 'spin' },
  { key: 'balloon', labelEn: 'Balloon', labelAr: 'بالونة', emoji: '🎈', keywords: ['balloon','بالونة','بلونه','بالون'], animation: 'float' },
  { key: 'musical', labelEn: 'Musical toy', labelAr: 'لعبة موسيقية', emoji: '🎵', keywords: ['music','musical','موسيقى','مزيكا','آلة'], animation: 'bounce' },
];

export function findToyFromInput(input: string): ToyDefinition | null {
  const s = input.toLowerCase().trim();
  // Normalize Arabic
  const normalized = s.replace(/ة/g, 'ه').replace(/ى/g, 'ي');
  for (const toy of TOYS) {
    if (toy.keywords.some(k => {
      const kn = k.toLowerCase().replace(/ة/g, 'ه').replace(/ى/g, 'ي');
      return normalized.includes(kn) || s.includes(k.toLowerCase());
    })) {
      return toy;
    }
  }
  return null;
}

export function getArabicCommandExamples(): string[] {
  return [
    'امسكي عربية لعبة',
    'امسكي كرة',
    'امسكي دبدوب',
    'امسكي كتاب',
    'امسكي صاروخ',
    'امسكي وردة',
    'امسكي بازل',
    'امسكي بالونة',
    'كلميني بالعربي',
    'أنا زعلان',
    'عايز أذاكر',
    'افتحي يوتيوب',
    'شغلي وقت التركيز',
    'اعملي حركة'
  ];
}

export function getEnglishCommandExamples(): string[] {
  return [
    'Hold a toy car',
    'Hold a ball',
    'Hold a teddy bear',
    'Hold a story book',
    'Hold a rocket',
    'Hold a flower',
    'Hold a puzzle',
    'Hold a balloon',
    'Talk to me in Arabic',
    'I am sad',
    'I want to study',
    'Open YouTube',
    'Start focus time',
    'Do a dance'
  ];
}

export interface PlayEvent {
  toy: ToyDefinition;
  timestamp: number;
  source: 'typed' | 'voice' | 'chip';
  mood: 'playful' | 'cozy' | 'happy';
  speechAr: string;
  speechEn: string;
}

export function createPlayEvent(toy: ToyDefinition, source: PlayEvent['source'] = 'typed'): PlayEvent {
  return {
    toy,
    timestamp: Date.now(),
    source,
    mood: 'playful',
    speechAr: `شوفي! أنا ماسكة ${toy.labelAr} ${toy.emoji}`,
    speechEn: `Look! I'm holding a ${toy.labelEn.toLowerCase()} ${toy.emoji}`
  };
}
