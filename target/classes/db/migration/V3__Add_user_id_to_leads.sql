-- 1. Add column nullable
ALTER TABLE public.leads
ADD COLUMN user_id BIGINT;

-- 2. Populate existing records
UPDATE public.leads
SET user_id = 1
WHERE user_id IS NULL;

-- 3. Make column NOT NULL
ALTER TABLE public.leads
ALTER COLUMN user_id SET NOT NULL;

-- 4. Add foreign key
ALTER TABLE public.leads
ADD CONSTRAINT fk_leads_user
FOREIGN KEY (user_id)
REFERENCES public.users(id);

-- 5. Add index
CREATE INDEX idx_leads_user ON public.leads (user_id);
