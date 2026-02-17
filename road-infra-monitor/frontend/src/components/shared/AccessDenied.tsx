import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ShieldX, Home, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface AccessDeniedProps {
  title?: string;
  description?: string;
  showHomeButton?: boolean;
  showBackButton?: boolean;
}

export function AccessDenied({
  title = 'Access Denied',
  description = 'You do not have permission to access this page.',
  showHomeButton = true,
  showBackButton = true,
}: AccessDeniedProps) {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-[calc(100vh-200px)] items-center justify-center p-6">
      <Card className="max-w-md text-center">
        <CardHeader>
          <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-red-100">
            <ShieldX className="h-10 w-10 text-red-600" />
          </div>
          <CardTitle className="text-2xl">{title}</CardTitle>
          <CardDescription className="text-base">{description}</CardDescription>
        </CardHeader>
        <CardContent className="flex gap-2 justify-center">
          {showBackButton && (
            <Button variant="outline" onClick={() => navigate(-1)} className="gap-2">
              <ArrowLeft className="h-4 w-4" />
              Go Back
            </Button>
          )}
          {showHomeButton && (
            <Button onClick={() => navigate('/')} className="gap-2">
              <Home className="h-4 w-4" />
              Dashboard
            </Button>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

// Full page version for routes
export function AccessDeniedPage() {
  return (
    <div className="min-h-screen bg-background">
      <AccessDenied />
    </div>
  );
}
