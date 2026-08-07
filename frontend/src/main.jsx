import React, {useEffect, useMemo, useState} from 'react';
import { createRoot } from 'react-dom/client';
import { Gamepad2, Play, Square, Clock3, Banknote, Monitor, RotateCw, Plus, TimerReset, Users, User, Trophy, X } from 'lucide-react';
import './styles.css';

const API='http://localhost:8080/api';
const money = v => `${Number(v || 0).toFixed(2)} EGP`;
const pad = n => String(n).padStart(2,'0');
const formatSeconds = total => { const sec=Math.max(0,Math.floor(total)); return `${pad(Math.floor(sec/3600))}:${pad(Math.floor((sec%3600)/60))}:${pad(sec%60)}`; };
const elapsedSeconds = start => Math.max(0,Math.floor((Date.now()-new Date(start).getTime())/1000));
const remainingSeconds = session => {
  if(session.sessionType==='MATCH' && session.currentMatchExpiresAt) return Math.floor((new Date(session.currentMatchExpiresAt).getTime()-Date.now())/1000);
  if(!session.plannedMinutes) return null;
  return Math.floor((new Date(session.startTime).getTime()+session.plannedMinutes*60000-Date.now())/1000);
};
async function request(url, options){ const res=await fetch(url,options); if(!res.ok){let msg=`Request failed (${res.status})`;try{const b=await res.json();if(b?.message)msg=b.message}catch{}throw new Error(msg)} return res.status===204?null:res.json(); }

function App(){
 const [devices,setDevices]=useState([]), [sessions,setSessions]=useState([]), [summary,setSummary]=useState({}), [pricing,setPricing]=useState([]);
 const [error,setError]=useState(''), [busy,setBusy]=useState(null), [chooser,setChooser]=useState(null);
 const [,setTick]=useState(0);
 async function load(){try{const[d,s,m,p]=await Promise.all([request(`${API}/devices`),request(`${API}/sessions/active`),request(`${API}/dashboard/today`),request(`${API}/pricing`)]);setDevices(d);setSessions(s);setSummary(m);setPricing(p);setError('')}catch(e){setError(e.message.includes('fetch')?'Backend is not reachable. Start Spring Boot on port 8080.':e.message)}}
 useEffect(()=>{load();const t=setInterval(()=>setTick(x=>x+1),1000),r=setInterval(load,3000);return()=>{clearInterval(t);clearInterval(r)}},[]);
 const activeByDevice=useMemo(()=>Object.fromEntries(sessions.map(s=>[s.device.id,s])),[sessions]);
 const getPrice=(device,type)=>pricing.find(p=>p.deviceType===device.type&&p.sessionType===type);
 async function action(key,fn){setBusy(key);setError('');try{await fn();setChooser(null);await load()}catch(e){setError(e.message)}finally{setBusy(null)}}
 const start=(device,type,plannedMinutes=null,matchCount=null)=>action(`start-${device.id}`,()=>request(`${API}/sessions/start`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({deviceId:device.id,sessionType:type,plannedMinutes,matchCount})}));
 const stop=id=>action(`stop-${id}`,()=>request(`${API}/sessions/${id}/stop`,{method:'POST'}));
 const extend=(id,minutes)=>action(`extend-${id}`,()=>request(`${API}/sessions/${id}/extend`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({minutes})}));
 const finishMatch=id=>action(`finish-${id}`,()=>request(`${API}/sessions/${id}/match/finish`,{method:'POST'}));
 const addMatch=id=>action(`addmatch-${id}`,()=>request(`${API}/sessions/${id}/match/add`,{method:'POST'}));
 return <div className="app">
  <header><div><div className="eyebrow">CONTROL CENTER</div><h1><Gamepad2/> PlayStation Cafe</h1><p>Sessions, match limits and live billing</p></div><button className="refresh" onClick={load}><RotateCw size={16}/> Refresh</button></header>
  {error&&<div className="error">{error}</div>}
  <section className="summary"><Stat icon={<Monitor/>} label="Devices" value={summary.totalDevices??devices.length}/><Stat icon={<Play/>} label="Playing now" value={summary.activeSessions??sessions.length}/><Stat icon={<Clock3/>} label="Completed today" value={summary.completedSessionsToday??0}/><Stat icon={<Banknote/>} label="Revenue today" value={money(summary.revenueToday)}/></section>
  <section className="grid">{devices.map(d=>{
    const s=activeByDevice[d.id], elapsed=s?elapsedSeconds(s.startTime):0, remain=s?remainingSeconds(s):null;
    const isMatch=s?.sessionType==='MATCH';
    const current=isMatch?Number(s.unitPriceSnapshot||0)*Number(s.purchasedMatches||1):s?Number(s.unitPriceSnapshot||s.hourlyRateSnapshot||0)*(Math.min(elapsed,s.plannedMinutes? s.plannedMinutes*60:elapsed)/3600):0;
    const ending=remain!==null&&remain>0&&remain<=120, expired=isMatch&&(Boolean(s.matchExpired)||remain<=0);
    return <article className={`card ${s?'playing':''} ${ending?'ending':''} ${expired?'expired':''}`} key={d.id}>
      <div className="cardtop"><div><span className={`pill ${d.type.toLowerCase()}`}>{d.type}</span><h2>{d.name}</h2></div><span className={`state ${s?'live':''}`}>{expired?'MATCH EXPIRED':s?'PLAYING':d.status}</span></div>
      {!s ? <><div className="idle"><div><strong>Ready for next session</strong><span>Choose Single, Multi or Match</span></div></div><button className="chooseBtn" onClick={()=>setChooser(d)}>Start session</button></> : <>
       <div className="modeLine"><span className={`modeBadge ${s.sessionType?.toLowerCase()}`}>{s.sessionType||'LEGACY'}</span><strong>{isMatch?`${money(s.unitPriceSnapshot)} / match`:`${money(s.unitPriceSnapshot||s.hourlyRateSnapshot)} / hour`}</strong></div>
       <div className="sessionClock"><div><span className="clockLabel">ELAPSED</span><div className="timer">{formatSeconds(elapsed)}</div></div>{remain!==null&&<div className={`remaining ${ending||expired?'warning':''}`}><span className="clockLabel">{isMatch?'MATCH TIME':'REMAINING'}</span><strong>{expired?'00:00:00':formatSeconds(remain)}</strong></div>}</div>
       {isMatch?<><div className="meta"><span>Matches</span><strong>{s.completedMatches||0} completed / {s.purchasedMatches||1} booked</strong></div><div className="meta"><span>Max per match</span><strong>{s.matchDurationMinutesSnapshot||15} min</strong></div><div className="meta"><span>Session total</span><strong className="cost">{money(current)}</strong></div><div className="actions matchActions"><button disabled={busy!==null} onClick={()=>addMatch(s.id)}><Plus size={14}/> Add match</button><button disabled={busy!==null} className={expired?'urgent':''} onClick={()=>finishMatch(s.id)}><Trophy size={14}/>{(s.completedMatches||0)+1 >= (s.purchasedMatches||1)?'Finish session':'Finish match'}</button><button disabled={busy!==null} className="stop" onClick={()=>stop(s.id)}><Square size={14}/>Stop</button></div>{expired&&<div className="expiredNote">Time limit reached. Finish this match before continuing.</div>}</>:
       <><div className="meta"><span>Session</span><strong>{s.plannedMinutes?`${s.plannedMinutes} min`:'Open'}</strong></div><div className="meta"><span>Current cost</span><strong className="cost">{money(current)}</strong></div><div className="actions">{s.plannedMinutes?<><button disabled={busy!==null} onClick={()=>extend(s.id,30)}><Plus size={14}/>30 min</button><button disabled={busy!==null} onClick={()=>extend(s.id,60)}><Plus size={14}/>1 hour</button></>:<div className="openHint"><TimerReset size={15}/> No time limit</div>}<button disabled={busy!==null} className="stop" onClick={()=>stop(s.id)}><Square size={15}/> Stop</button></div></>}
      </>}
    </article>
  })}</section>
  {chooser&&<SessionChooser device={chooser} getPrice={getPrice} busy={busy} onClose={()=>setChooser(null)} onStart={start}/>}
 </div>
}

function SessionChooser({device,getPrice,busy,onClose,onStart}){const [mode,setMode]=useState(null),[matches,setMatches]=useState(1);const price=mode?getPrice(device,mode):null;return <div className="overlay" onMouseDown={e=>e.target===e.currentTarget&&onClose()}><div className="modal"><div className="modalHead"><div><span>{device.type}</span><h2>{device.name}</h2></div><button className="iconBtn" onClick={onClose}><X/></button></div>{!mode?<><p className="modalLead">Choose billing type</p><div className="modeChoices"><button onClick={()=>setMode('SINGLE')}><User/><strong>Single</strong><small>{money(getPrice(device,'SINGLE')?.price)} / hour</small></button><button onClick={()=>setMode('MULTI')}><Users/><strong>Multi</strong><small>{money(getPrice(device,'MULTI')?.price)} / hour</small></button><button onClick={()=>setMode('MATCH')}><Trophy/><strong>Match</strong><small>{money(getPrice(device,'MATCH')?.price)} / match</small></button></div></>:mode==='MATCH'?<><button className="backlink" onClick={()=>setMode(null)}>← Change mode</button><div className="matchSetup"><Trophy size={36}/><h3>Match session</h3><p>{money(price?.price)} per match · maximum {price?.matchDurationMinutes||15} minutes each</p><div className="stepper"><button onClick={()=>setMatches(Math.max(1,matches-1))}>−</button><div><strong>{matches}</strong><span>match{matches>1?'es':''}</span></div><button onClick={()=>setMatches(matches+1)}>+</button></div><div className="quote"><span>Session total</span><strong>{money(Number(price?.price||0)*matches)}</strong></div><button className="launch" disabled={busy!==null} onClick={()=>onStart(device,'MATCH',null,matches)}><Play size={16}/> Start match timer</button></div></>:<><button className="backlink" onClick={()=>setMode(null)}>← Change mode</button><div className="hourSetup"><div className="selectedMode"><strong>{mode}</strong><span>{money(price?.price)} / hour</span></div><p>Choose a duration or keep the session open.</p><div className="durationChoices"><button onClick={()=>onStart(device,mode,30,null)}>30 min</button><button onClick={()=>onStart(device,mode,60,null)}>1 hour</button><button className="launch" onClick={()=>onStart(device,mode,null,null)}><Play size={15}/> Open</button></div></div></>}</div></div>}
function Stat({icon,label,value}){return <div className="stat"><div className="icon">{icon}</div><div><span>{label}</span><strong>{value}</strong></div></div>}
createRoot(document.getElementById('root')).render(<App/>);
