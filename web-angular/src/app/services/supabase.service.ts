import { Injectable } from '@angular/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { Tablas } from './tablas.supabase';

@Injectable({
  providedIn: 'root',
})
export class SupabaseService {

  tablas!: Tablas;
  private supabase: SupabaseClient;
  private supabaseAdmin: SupabaseClient;

  constructor() {
    this.supabase = createClient(
      'https://sgyidsdrwqkqtqimozvm.supabase.co',
      'sb_publishable_SOBQAb6mNQod2cCeG8QS-w_6XT1D-YJ' //publishable key
    );

    this.supabaseAdmin = createClient(
      'https://chdjeqqvuhqqxywiolmh.supabase.co',
      'sb_secret_6_U-B0yASzMKhFPOwz209w_INEwdpbx' //secret key
    );
  }

  async getAll(table: Tablas) {
    const { data, error } = await this.supabase
      .from(table)
      .select('*')
      .order("id");

    if (error) {
      throw error;
    }

    console.log(`GET ALL from '${table}': ${data.length}`)
    return data;
  }

  async find(table: Tablas, key: string, value: any) {
    const { data, error } = await this.supabase
      .from(table)
      .select('*')
      .eq(key, value);

    if (error) {
      throw error;
    }

    console.log(`FIND from '${table}': ${data.length}`)

    //Encontrado -> data.length == 1
    return data
  }

  async deleteRow(table: Tablas, key: string, value: any) {
    const { error } = await this.supabase
      .from(table)
      .delete()
      .eq(key, value);

    if (error) throw error;
    console.log(`DELETE from '${table}'`)
  }

  async insert(table: Tablas, value: any){
    const { id, created_at, ...payload } = value;

    // Elimina campos null/undefined para que Supabase use sus defaults
    const payload_filtered = Object.fromEntries(
      Object.entries(payload).filter(([_, v]) => v !== null && v !== undefined)
    );

    const { error } = await this.supabase
      .from(table)
      .insert([payload_filtered])

    if (error) throw error;
    console.log(`INSERTADO en '${table}'`)
  }

  async update(table: Tablas, keyID: string, valueID: string, value: any){
    const { id, ...payload } = value;

    const { error } = await this.supabase
      .from(table)
      .update([payload])
      .eq(keyID, valueID);

    if (error) {
      throw error;
    }

    console.log(`UPDATED '${table}' (${keyID} = ${valueID})`)
  }

}