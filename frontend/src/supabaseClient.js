import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = 'https://tjgyjtlmzotcapeqndpx.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_uRhZDC4AJHqD32W8aPaFkQ_2MS04NPs';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
