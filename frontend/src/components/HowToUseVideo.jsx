import React, { useState, useEffect, useRef } from 'react';
import { 
  Play, Pause, RotateCcw, Volume2, VolumeX, Subtitles, 
  Maximize, Minimize, Compass, Calendar, Navigation, CloudRain, Users 
} from 'lucide-react';
import './HowToUseVideo.css';

const STEPS = [
  {
    id: 1,
    timeStart: 0,
    timeEnd: 22,
    stepBadge: "Step 01 • 0:00 - 0:22",
    title: "Create Account & Start Planning",
    subtitle: "Fast authentication & 1-click trip onboarding",
    caption: "Sign up or log in to TravelMate AI. From your personalized dashboard, click 'Plan New Trip' to begin your tailored journey.",
    icon: <Compass size={16} />
  },
  {
    id: 2,
    timeStart: 22,
    timeEnd: 44,
    stepBadge: "Step 02 • 0:22 - 0:44",
    title: "Select Destination, Dates, Pace & Budget",
    subtitle: "Relaxed (1-2), Moderate (3-4), Fast-Paced (5-6 places/day)",
    caption: "Enter your Indian destination, select travel dates, set your budget in INR, and choose your preferred travel pace.",
    icon: <Calendar size={16} />
  },
  {
    id: 3,
    timeStart: 44,
    timeEnd: 66,
    stepBadge: "Step 03 • 0:44 - 1:06",
    title: "Geographical Route Optimization & Map",
    subtitle: "Nearest Neighbour + 2-Opt route order, zero backtracking",
    caption: "AI arranges attractions consecutively in optimal geographical visiting order, syncing numbered map markers with the live road route.",
    icon: <Navigation size={16} />
  },
  {
    id: 4,
    timeStart: 66,
    timeEnd: 88,
    stepBadge: "Step 04 • 1:06 - 1:28",
    title: "Live Weather & 1-Click Rain Adaptation",
    subtitle: "Forecast intelligence with instant indoor cultural swaps",
    caption: "TravelMate checks live forecasts. If rainfall is expected, tap 'Find Indoor Alternatives' to swap outdoor spots with covered landmarks.",
    icon: <CloudRain size={16} />
  },
  {
    id: 5,
    timeStart: 88,
    timeEnd: 111,
    stepBadge: "Step 05 • 1:28 - 1:51",
    title: "Invite Travellers & Plan Together",
    subtitle: "Collaborative trip planning with Editor & Viewer roles",
    caption: "Tap '+ Invite Travellers' to share the trip with family or friends by email. Coordinate schedules and collaborate in real time.",
    icon: <Users size={16} />
  }
];

const DEFAULT_DURATION = 110.67;

const HowToUseVideo = () => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(DEFAULT_DURATION);
  const [isMuted, setIsMuted] = useState(false);
  const [volume, setVolume] = useState(1);
  const [showCaptions, setShowCaptions] = useState(true);
  const [isFullscreen, setIsFullscreen] = useState(false);

  const videoRef = useRef(null);
  const playerCardRef = useRef(null);

  // Active step based on video playback time
  const currentStep = STEPS.find(s => currentTime >= s.timeStart && currentTime < s.timeEnd) || STEPS[STEPS.length - 1];

  const handleTimeUpdate = () => {
    if (videoRef.current) {
      setCurrentTime(videoRef.current.currentTime);
    }
  };

  const handleLoadedMetadata = () => {
    if (videoRef.current && videoRef.current.duration) {
      setDuration(videoRef.current.duration);
    }
  };

  const handleEnded = () => {
    setIsPlaying(false);
  };

  const togglePlay = () => {
    if (!videoRef.current) return;
    if (videoRef.current.paused || videoRef.current.ended) {
      if (videoRef.current.ended) {
        videoRef.current.currentTime = 0;
      }
      videoRef.current.play().then(() => {
        setIsPlaying(true);
      }).catch(err => {
        console.warn('Playback error:', err);
      });
    } else {
      videoRef.current.pause();
      setIsPlaying(false);
    }
  };

  const handleRestart = () => {
    if (videoRef.current) {
      videoRef.current.currentTime = 0;
      setCurrentTime(0);
      videoRef.current.play().then(() => {
        setIsPlaying(true);
      }).catch(err => {
        console.warn('Restart playback error:', err);
      });
    }
  };

  const handleSeek = (e) => {
    const val = parseFloat(e.target.value);
    setCurrentTime(val);
    if (videoRef.current) {
      videoRef.current.currentTime = val;
    }
  };

  const handleJumpToStep = (step) => {
    if (videoRef.current) {
      videoRef.current.currentTime = step.timeStart;
      setCurrentTime(step.timeStart);
      videoRef.current.play().then(() => {
        setIsPlaying(true);
      }).catch(err => {
        console.warn('Step jump playback error:', err);
      });
    }
  };

  const toggleMute = () => {
    if (videoRef.current) {
      const nextMuted = !isMuted;
      videoRef.current.muted = nextMuted;
      setIsMuted(nextMuted);
    }
  };

  const handleVolumeChange = (e) => {
    const val = parseFloat(e.target.value);
    setVolume(val);
    if (videoRef.current) {
      videoRef.current.volume = val;
      const muted = val === 0;
      videoRef.current.muted = muted;
      setIsMuted(muted);
    }
  };

  const toggleFullscreen = () => {
    if (!playerCardRef.current) return;
    if (!document.fullscreenElement) {
      playerCardRef.current.requestFullscreen?.().catch(err => {
        console.error('Error attempting fullscreen:', err);
      });
    } else {
      document.exitFullscreen?.().catch(err => {
        console.error('Error exiting fullscreen:', err);
      });
    }
  };

  useEffect(() => {
    const handleFsChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', handleFsChange);
    return () => document.removeEventListener('fullscreenchange', handleFsChange);
  }, []);

  const formatTime = (seconds) => {
    if (isNaN(seconds) || seconds === null) return '00:00';
    const totalSecs = Math.floor(seconds);
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins < 10 ? '0' : ''}${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <section className="how-to-use-section padding-section" id="how-to-use-section">
      <div className="section-header">
        <div className="header-tag">Interactive Product Walkthrough</div>
        <h2>How to Use TravelMate</h2>
        <p>Watch this guided walkthrough video to discover how TravelMate AI creates route-optimized, weather-adapted Indian itineraries.</p>
      </div>

      {/* Step Navigation Pills */}
      <div className="video-step-pills">
        {STEPS.map((step, idx) => {
          const isActive = currentStep.id === step.id;
          return (
            <button
              key={step.id}
              className={`step-pill-btn ${isActive ? 'active' : ''}`}
              onClick={() => handleJumpToStep(step)}
              title={`Jump to ${step.title}`}
            >
              <span className="pill-num">0{idx + 1}</span>
              <span className="pill-label">{step.title}</span>
            </button>
          );
        })}
      </div>

      {/* Video Player Card */}
      <div className="video-player-card glass-panel" ref={playerCardRef}>
        
        {/* VIDEO DISPLAY AREA */}
        <div className="video-viewport">
          
          {/* HTML5 Native Video Player */}
          <video
            ref={videoRef}
            className="travelmate-screen-video"
            playsInline
            preload="metadata"
            onTimeUpdate={handleTimeUpdate}
            onLoadedMetadata={handleLoadedMetadata}
            onEnded={handleEnded}
            onClick={togglePlay}
          >
            <source src="/videos/TravelMate_video_email_blurred.mp4" type="video/mp4" />
            <source src="/videos/Screen%20Recording%202026-11-15%20162505.mp4" type="video/mp4" />
            Your browser does not support the video tag.
          </video>

          {/* CLOSED CAPTIONS / STEP OVERLAY */}
          {showCaptions && currentStep && (
            <div className="video-captions-overlay animate-fade-in">
              <div className="captions-text-box">
                <span className="cc-badge">{currentStep.stepBadge}</span>
                <span className="cc-text">{currentStep.caption}</span>
              </div>
            </div>
          )}

          {/* Big Center Play Button Overlay when paused */}
          {!isPlaying && (
            <div className="video-center-play-overlay" onClick={togglePlay}>
              <button className="center-play-btn" title="Play Walkthrough" aria-label="Play Walkthrough">
                <Play size={36} fill="#ffffff" />
              </button>
              <span className="play-cta-text">Click to Play Walkthrough</span>
            </div>
          )}

        </div>

        {/* MODERN VIDEO CONTROLS BAR */}
        <div className="video-controls-bar">
          
          {/* Progress / Scrub Bar */}
          <div className="scrub-bar-container">
            <input 
              type="range" 
              min="0" 
              max={duration || DEFAULT_DURATION} 
              step="0.1"
              value={currentTime}
              onChange={handleSeek}
              className="video-seek-slider"
              title="Seek timeline"
            />
            <div className="scrub-progress-fill" style={{ width: `${progressPercent}%` }}></div>
            {/* Step Markers along scrub bar */}
            {STEPS.map(s => (
              <span 
                key={s.id} 
                className="timeline-step-marker" 
                style={{ left: `${(s.timeStart / (duration || DEFAULT_DURATION)) * 100}%` }}
                title={`${s.title} (${s.stepBadge})`}
              />
            ))}
          </div>

          {/* Control Buttons & Counters */}
          <div className="controls-row">
            <div className="controls-left">
              <button 
                className="control-icon-btn play-btn" 
                onClick={togglePlay} 
                title={isPlaying ? "Pause" : "Play"}
                aria-label={isPlaying ? "Pause" : "Play"}
              >
                {isPlaying ? <Pause size={18} /> : <Play size={18} fill="currentColor" />}
              </button>

              <button 
                className="control-icon-btn" 
                onClick={handleRestart} 
                title="Restart from beginning"
                aria-label="Restart from beginning"
              >
                <RotateCcw size={16} />
              </button>

              {/* Volume / Mute Control */}
              <div className="volume-control-group">
                <button 
                  className={`control-icon-btn ${isMuted ? 'active' : ''}`}
                  onClick={toggleMute}
                  title={isMuted ? "Unmute" : "Mute"}
                  aria-label={isMuted ? "Unmute" : "Mute"}
                >
                  {isMuted || volume === 0 ? <VolumeX size={17} /> : <Volume2 size={17} />}
                </button>
                <input 
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  value={isMuted ? 0 : volume}
                  onChange={handleVolumeChange}
                  className="volume-slider"
                  title="Adjust Volume"
                  aria-label="Adjust Volume"
                />
              </div>

              {/* Time Display */}
              <div className="video-time-display">
                <span className="time-current">{formatTime(currentTime)}</span>
                <span className="time-divider">/</span>
                <span className="time-total">{formatTime(duration)}</span>
              </div>
            </div>

            <div className="controls-right">
              <button 
                className={`control-icon-btn ${showCaptions ? 'active' : ''}`}
                onClick={() => setShowCaptions(!showCaptions)}
                title={showCaptions ? "Disable Captions (CC)" : "Enable Captions (CC)"}
                aria-label={showCaptions ? "Disable Captions (CC)" : "Enable Captions (CC)"}
              >
                <Subtitles size={17} />
              </button>

              <button 
                className={`control-icon-btn ${isFullscreen ? 'active' : ''}`}
                onClick={toggleFullscreen}
                title={isFullscreen ? "Exit Fullscreen" : "Fullscreen"}
                aria-label={isFullscreen ? "Exit Fullscreen" : "Fullscreen"}
              >
                {isFullscreen ? <Minimize size={17} /> : <Maximize size={17} />}
              </button>

              <span className="badge-duration">Full Walkthrough</span>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
};

export default HowToUseVideo;
