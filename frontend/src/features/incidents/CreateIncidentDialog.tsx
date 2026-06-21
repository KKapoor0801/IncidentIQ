import { useState, type FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { incidentsApi } from '@/api/incidents';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Textarea } from '@/components/ui/Textarea';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';

interface CreateIncidentDialogProps {
  open: boolean;
  onClose: () => void;
}

export function CreateIncidentDialog({ open, onClose }: CreateIncidentDialogProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [fieldErrors, setFieldErrors] = useState<{ title?: string; description?: string }>({});
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const mutation = useMutation({
    mutationFn: incidentsApi.create,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['incidents'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast('success', 'Incident created successfully');
      handleClose();
    },
    onError: () => {
      toast('error', 'Failed to create incident');
    },
  });

  function validate(): boolean {
    const errors: { title?: string; description?: string } = {};
    if (!title.trim()) errors.title = 'Title is required';
    else if (title.trim().length < 3) errors.title = 'Title must be at least 3 characters';
    else if (title.trim().length > 255) errors.title = 'Title must be at most 255 characters';
    if (!description.trim()) errors.description = 'Description is required';
    else if (description.trim().length < 10)
      errors.description = 'Description must be at least 10 characters';
    else if (description.trim().length > 5000)
      errors.description = 'Description must be at most 5000 characters';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!validate()) return;
    mutation.mutate({ title: title.trim(), description: description.trim() });
  }

  function handleClose() {
    setTitle('');
    setDescription('');
    setFieldErrors({});
    onClose();
  }

  return (
    <Dialog open={open} onClose={handleClose} title="Create Incident">
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="Title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          error={fieldErrors.title}
          placeholder="Brief description of the incident"
        />
        <Textarea
          label="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          error={fieldErrors.description}
          placeholder="Detailed description of what happened..."
          rows={5}
        />
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="secondary" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" loading={mutation.isPending}>
            Create Incident
          </Button>
        </div>
      </form>
    </Dialog>
  );
}
