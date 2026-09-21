const CLIPS = {hello:'/assets/welcome.mp3', pat:'/assets/pat.mp3', cheer:'/assets/cheer.mp3', focus:'/assets/focus.mp3', rest:'/assets/rest.mp3'};
export class MiyuAudio {
  constructor(prefs, onSpeaking, onError) {
    this.prefs=prefs;this.onSpeaking=onSpeaking;this.onError=onError;this.clip=null;this.context=null;this.noise=null;this.gain=null;this.ambientName='none';
  }
  update(prefs){this.prefs=prefs;if(this.clip)this.clip.volume=prefs.muted?0:prefs.volume/100;if(prefs.muted)this.stopVoice();if(this.gain&&this.context)this.gain.gain.setTargetAtTime(prefs.muted?0:prefs.ambientVolume/100*prefs.volume/100*.22,this.context.currentTime,.15);}
  stopVoice(){if(this.clip){this.clip.pause();this.clip.currentTime=0;this.clip=null;}if('speechSynthesis' in window)window.speechSynthesis.cancel();this.onSpeaking(false);}
  async play(name){
    if(this.prefs.muted)return;
    this.stopVoice();if(!CLIPS[name])return;
    const clip=new Audio(CLIPS[name]);this.clip=clip;clip.volume=this.prefs.volume/100;
    clip.onplaying=()=>this.onSpeaking(true);
    clip.onended=()=>{this.onSpeaking(false);if(this.clip===clip)this.clip=null;};
    clip.onerror=()=>{this.onSpeaking(false);this.onError?.('This voice clip could not play. Check your audio device.');};
    try{await clip.play();}catch(e){this.onSpeaking(false);if(e.name!=='AbortError')this.onError?.('Tap Say hello to enable audio in this browser.');}
  }
  speak(text, force=false){
    if(this.prefs.muted||(!force&&!this.prefs.speakReplies))return;
    if(!('speechSynthesis' in window)){this.onError?.('Speech playback isn’t available here. Studio voice reactions still work.');return;}
    this.stopVoice();const utterance=new SpeechSynthesisUtterance(text.slice(0,2600));
    const voices=speechSynthesis.getVoices();const wantsArabic=document.documentElement.lang==='ar';const voice=voices.find(v=>v.voiceURI===this.prefs.voice)||voices.find(v=>wantsArabic&&/^ar[-_]/i.test(v.lang))||voices.find(v=>!wantsArabic&&/en[-_]/i.test(v.lang)&&/female|samantha|aria|jenny|zira|natural/i.test(v.name))||voices.find(v=>wantsArabic?/^ar/i.test(v.lang):/^en/i.test(v.lang));
    if(voice)utterance.voice=voice;utterance.lang=wantsArabic?'ar-SA':'en-US';utterance.rate=.97;utterance.pitch=1.04;utterance.volume=this.prefs.volume/100;
    utterance.onstart=()=>this.onSpeaking(true);utterance.onend=()=>this.onSpeaking(false);
    utterance.onerror=e=>{this.onSpeaking(false);if(!['interrupted','canceled'].includes(e.error))this.onError?.('Device speech is unavailable. Try another voice in Settings.');};
    speechSynthesis.speak(utterance);
  }
  async ensureContext(){if(!this.context){const C=window.AudioContext||window.webkitAudioContext;if(!C)throw new Error('Web Audio is unavailable.');this.context=new C();}if(this.context.state==='suspended')await this.context.resume();return this.context;}
  async ambient(name){
    if(this.noise){try{this.noise.stop();}catch{}this.noise.disconnect();this.noise=null;}
    if(this.lfo){try{this.lfo.stop();}catch{}this.lfo.disconnect();this.lfo=null;}
    if(this.gain){this.gain.disconnect();this.gain=null;}this.ambientName=name;
    if(name==='none')return;
    const c=await this.ensureContext();const buffer=c.createBuffer(1,c.sampleRate*4,c.sampleRate);const data=buffer.getChannelData(0);let brown=0;
    for(let i=0;i<data.length;i++){const white=Math.random()*2-1;brown=(brown+white*.02)/1.02;data[i]=name==='breeze'?brown*3.5:white*.27+brown*.6;}
    this.noise=c.createBufferSource();this.noise.buffer=buffer;this.noise.loop=true;
    const filter=c.createBiquadFilter();filter.type='lowpass';filter.frequency.value=name==='rain'?3600:800;
    this.gain=c.createGain();this.gain.gain.value=this.prefs.muted?0:this.prefs.ambientVolume/100*this.prefs.volume/100*.22;
    this.noise.connect(filter);filter.connect(this.gain);this.gain.connect(c.destination);this.noise.start();
    if(name==='breeze'){this.lfo=c.createOscillator();const depth=c.createGain();depth.gain.value=260;this.lfo.frequency.value=.10;this.lfo.connect(depth);depth.connect(filter.frequency);this.lfo.start();}
  }
  async chime(){
    if(this.prefs.muted)return;
    try{const c=await this.ensureContext();[523.25,659.25,783.99].forEach((f,i)=>{const o=c.createOscillator(),g=c.createGain();o.type='sine';o.frequency.value=f;g.gain.setValueAtTime(0,c.currentTime+i*.13);g.gain.linearRampToValueAtTime(this.prefs.volume/100*.055,c.currentTime+i*.13+.025);g.gain.exponentialRampToValueAtTime(.0001,c.currentTime+i*.13+1.1);o.connect(g);g.connect(c.destination);o.start(c.currentTime+i*.13);o.stop(c.currentTime+i*.13+1.2);});}catch{}
  }
}
