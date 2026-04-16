import { Injectable } from '@angular/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { Tablas } from './tablas.supabase';

@Injectable({
  providedIn: 'root',
})
export class SupabaseService {

  tablas!: Tablas;
  private supabase: SupabaseClient;

  constructor() {
    this.supabase = createClient(
      'https://sgyidsdrwqkqtqimozvm.supabase.co',
      'sb_publishable_SOBQAb6mNQod2cCeG8QS-w_6XT1D-YJ' //publishable key
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

  async insert(table: Tablas, value: any) {
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

  async update(table: Tablas, keyID: string, valueID: string, value: any) {
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

  async subirAvatarUsuario(keyID: string, valueID: string, imagen: File) {
    const imagenExt = imagen.name.split('.').pop();
    const imagenNombre = `${Math.random()}.${imagenExt}`;
    const imagenPath = `avatares/${imagenNombre}`;

    // 1. Subir el archivo
    let { error } = await this.supabase.storage
      .from('imagenes')
      .upload(imagenPath, imagen);

    // 2. Comprobar el error ANTES de continuar
    if (error) {
      throw error;
    }

    // 3. Obtener la URL pública
    const { data } = this.supabase.storage
      .from('imagenes')
      .getPublicUrl(imagenPath);

    const urlPublica = data.publicUrl;

    // 4. Guardar en la BD
    await this.update(Tablas.USUARIOS, keyID, valueID, { avatar_url: urlPublica });
  }
}