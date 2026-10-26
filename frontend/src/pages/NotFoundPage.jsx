import { ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return <main className="not-found"><span>404</span><h1>This page wandered off.</h1><p>The address may have changed, or it may not exist.</p><Link className="dark-action" to="/"><ArrowLeft size={16} /> Back to PGConnect</Link></main>;
}
