import { Injectable } from '@angular/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';

@Injectable({
  providedIn: 'root',
})
export class SupabaseService {
  private supabase: SupabaseClient;

  constructor() {
    this.supabase = createClient(
      'https://chdjeqqvuhqqxywiolmh.supabase.co',
      'sb_publishable_fUIe-BG42kTWDI6KneEnMw_E2PEBAkR'
    );
  }

  async getAllUsers() {
    const { data, error } = await this.supabase
      .from('USERS')
      .select('*');

    if (error) {
      throw error;
    }

    console.log("GET ALL USERS: "+data.length)
    return data;
  }
}