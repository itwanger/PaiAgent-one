import { useRef, useState, useEffect } from 'react';
import { Card, Button, Slider, Space } from 'antd';
import { PlayCircleOutlined, PauseCircleOutlined, DownloadOutlined } from '@ant-design/icons';

interface AudioPlayerProps {
  audioUrl: string;
  fileName?: string;
}

/**
 * 音频播放器组件
 */
const AudioPlayer = ({ audioUrl, fileName }: AudioPlayerProps) => {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);

  // 处理音频 URL - 如果是相对路径,加上后端地址
  const fullAudioUrl = audioUrl.startsWith('/') 
    ? `http://localhost:8080${audioUrl}` 
    : audioUrl;

  // 格式化时间显示
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  // 播放/暂停切换
  const togglePlayPause = async () => {
    if (audioRef.current) {
      try {
        if (isPlaying) {
          audioRef.current.pause();
          setIsPlaying(false);
        } else {
          await audioRef.current.play();
          setIsPlaying(true);
        }
      } catch (error) {
        console.error('播放失败:', error);
        setIsPlaying(false);
      }
    }
  };

  // 处理进度条拖动
  const handleSliderChange = (value: number) => {
    if (audioRef.current) {
      audioRef.current.currentTime = value;
      setCurrentTime(value);
    }
  };

  // 下载音频文件
  const handleDownload = () => {
    const link = document.createElement('a');
    link.href = audioUrl;
    link.download = fileName || 'audio.mp3';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // 监听音频播放时间更新
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;

    const updateTime = () => setCurrentTime(audio.currentTime);
    const updateDuration = () => setDuration(audio.duration);
    const handleEnded = () => setIsPlaying(false);

    audio.addEventListener('timeupdate', updateTime);
    audio.addEventListener('loadedmetadata', updateDuration);
    audio.addEventListener('ended', handleEnded);

    return () => {
      audio.removeEventListener('timeupdate', updateTime);
      audio.removeEventListener('loadedmetadata', updateDuration);
      audio.removeEventListener('ended', handleEnded);
    };
  }, []);

  return (
    <Card title="🎵 音频播放器" size="small" className="mb-4">
      <audio ref={audioRef} src={fullAudioUrl} />
      
      <Space direction="vertical" className="w-full">
        {/* 播放控制按钮 */}
        <div className="flex items-center justify-center gap-4">
          <Button
            type="primary"
            shape="circle"
            size="large"
            icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
            onClick={togglePlayPause}
          />
        </div>

        {/* 进度条 */}
        <div className="px-2">
          <Slider
            value={currentTime}
            max={duration || 100}
            onChange={handleSliderChange}
            tooltip={{ formatter: (value) => formatTime(value || 0) }}
          />
          <div className="flex justify-between text-xs text-gray-500">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* 音频信息 */}
        {fileName && (
          <div className="text-xs text-gray-600 text-center">
            文件: {fileName}
          </div>
        )}
      </Space>
    </Card>
  );
};

export default AudioPlayer;