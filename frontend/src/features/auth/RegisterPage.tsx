import { useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

export function RegisterPage() {
  const { register, isAuthenticated, isLoading, error, clearError } = useAuthStore();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{
    fullName?: string;
    email?: string;
    password?: string;
  }>({});

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  function validate(): boolean {
    const errors: { fullName?: string; email?: string; password?: string } = {};
    if (!fullName.trim()) errors.fullName = 'Full name is required';
    if (!email) errors.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Invalid email address';
    if (!password) errors.password = 'Password is required';
    else if (password.length < 8) errors.password = 'Password must be at least 8 characters';
    else if (!/(?=.*[A-Za-z])(?=.*\d)/.test(password))
      errors.password = 'Must contain at least 1 letter and 1 number';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    clearError();
    if (!validate()) return;
    try {
      await register(email, password, fullName);
    } catch {
      // Error is already set in the store
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 dark:bg-gray-950">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-10 w-10 items-center justify-center rounded-xl bg-gray-900 dark:bg-white">
            <span className="text-sm font-bold text-white dark:text-gray-900">IQ</span>
          </div>
          <h1 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
            Create your account
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Get started with IncidentIQ
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300">
              {error}
            </div>
          )}

          <Input
            label="Full name"
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            error={fieldErrors.fullName}
            placeholder="Jane Doe"
            autoComplete="name"
          />

          <Input
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            error={fieldErrors.email}
            placeholder="you@example.com"
            autoComplete="email"
          />

          <Input
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={fieldErrors.password}
            placeholder="Min 8 chars, 1 letter + 1 number"
            autoComplete="new-password"
          />

          <Button type="submit" loading={isLoading} className="w-full">
            Create account
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link
            to="/login"
            className="font-medium text-gray-900 hover:underline dark:text-gray-100"
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
