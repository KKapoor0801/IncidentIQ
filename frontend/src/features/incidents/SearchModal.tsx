import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { incidentsApi } from '@/api/incidents';
import type { SearchResult } from '@/types';

interface SearchModalProps {
  open: boolean;
  onClose: () => void;
}

export function SearchModal({ open, onClose }: SearchModalProps) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

  useEffect(() => {
    if (open) {
      setQuery('');
      setResults([]);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  const search = useCallback(
    (q: string) => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      if (!q.trim()) {
        setResults([]);
        setLoading(false);
        return;
      }
      setLoading(true);
      debounceRef.current = setTimeout(async () => {
        try {
          const { data } = await incidentsApi.search(q.trim());
          setResults(data.content);
        } catch {
          setResults([]);
        } finally {
          setLoading(false);
        }
      }, 300);
    },
    []
  );

  function handleInputChange(value: string) {
    setQuery(value);
    search(value);
  }

  function handleSelect(result: SearchResult) {
    const id = result['id'] as string | undefined;
    if (id) {
      navigate(`/incidents/${id}`);
      onClose();
    }
  }

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-[20vh]">
      <div className="fixed inset-0 bg-black/50" onClick={onClose} />
      <div className="relative w-full max-w-lg rounded-xl border border-gray-200 bg-white shadow-2xl dark:border-gray-700 dark:bg-gray-900 mx-4">
        <div className="flex items-center gap-3 border-b border-gray-200 px-4 dark:border-gray-700">
          <svg
            className="h-5 w-5 shrink-0 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
            />
          </svg>
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => handleInputChange(e.target.value)}
            placeholder="Search incidents..."
            className="flex-1 bg-transparent py-3 text-sm text-gray-900 outline-none placeholder:text-gray-400 dark:text-gray-100 dark:placeholder:text-gray-500"
          />
          <kbd className="hidden rounded border border-gray-200 px-1.5 py-0.5 text-xs text-gray-400 sm:inline dark:border-gray-700">
            ESC
          </kbd>
        </div>

        <div className="max-h-72 overflow-y-auto p-2">
          {loading && (
            <div className="px-3 py-6 text-center text-sm text-gray-400">
              Searching...
            </div>
          )}
          {!loading && query.trim() && results.length === 0 && (
            <div className="px-3 py-6 text-center text-sm text-gray-400">
              No results found
            </div>
          )}
          {!loading &&
            results.map((result) => {
              const id = result['id'] as string | undefined;
              const title = result['title'] as string | undefined;
              const status = result['status'] as string | undefined;
              return (
                <button
                  key={id ?? Math.random()}
                  onClick={() => handleSelect(result)}
                  className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm hover:bg-gray-100 dark:hover:bg-gray-800"
                >
                  <span className="flex-1 truncate text-gray-900 dark:text-gray-100">
                    {title ?? 'Untitled'}
                  </span>
                  {status && (
                    <span className="shrink-0 text-xs text-gray-400">
                      {status}
                    </span>
                  )}
                </button>
              );
            })}
          {!loading && !query.trim() && (
            <div className="px-3 py-6 text-center text-sm text-gray-400">
              Type to search incidents
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
