import { Injectable } from '@angular/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';

@Injectable({
  providedIn: 'root',
})
export class SupabaseService {
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

  async getAllUsers() {
    const { data, error } = await this.supabase
      .from('usuarios')
      .select('*');

    if (error) {
      throw error;
    }

    console.log("GET ALL USERS: " + data.length)
    return data;
  }

  async findUser(email: string) {
    const { data, error } = await this.supabase
      .from('usuarios')
      .select('*')
      .eq('email', email);

    if (error) {
      throw error;
    }

    console.log("USER FOUND: " + data.length)

    //Encontrado -> data.length == 1
    return data
  }

  async createUser(_nombre: string, _apellido1: string, _apellido2: string, _passwd: string, _rol: string, _tel: string, _mail: string, _carnet: string[]) {

    const { data, error: authError } = await this.supabaseAdmin.auth.admin.createUser({
      email: _mail,
      password: _passwd,
      email_confirm: true
    });

    if (authError) throw authError;

    const uid = data.user?.id;
    if (!uid) throw new Error('No se genero el UID');
    console.log(`UID: ${uid}`)
    
    const { error } = await this.supabase
    .from('usuarios')
    .insert([
      {
        id: uid,
        nombre: _nombre,
        apellido1: _apellido1,
        apellido2: _apellido2,
        password: _passwd,
        rol: _rol,
        telefono: _tel,
        email: _mail,
        licencia_conducir: _carnet
      }
    ])
    
    if (error) throw error;
    console.log(`USUARIO CREADO: ${uid} / ${_mail}`)
  }
  
  async deleteUser(id: number) {
    const { error } = await this.supabase
      .from('usuarios')
      .delete()
      .eq('id', id);

    if (error) throw error;
  }
}