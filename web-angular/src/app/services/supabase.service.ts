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

    console.log("GET ALL USERS: " + data.length)
    return data;
  }

  async findUser(user: string) {
    const { data, error } = await this.supabase
      .from('USERS')
      .select('*')
      .eq('user', user);

    if (error) {
      throw error;
    }

    console.log("USER FOUND: " + data.length)

    //Encontrado -> data.length == 1
    return data
  }

  async createUser(_nombre:string,_apellido1:string,_apellido2:string,_usern: string, _passwd: string, _rol: string, _tel: string, _mail: string, _carnet: string[]){
    const { error } = await this.supabase
      .from('USERS')
      .insert([
        {
          nombre:_nombre,
          apellido1:_apellido1,
          apellido2:_apellido2,
          user: _usern,
          password: _passwd,
          role:_rol,
          tel:_tel,
          mail:_mail,
          licencia_conducir:_carnet
        }
      ])

      if (error) throw error;
  }

  async deleteUser(id: number) {
  const { error } = await this.supabase
    .from('USERS')
    .delete()
    .eq('id', id);

  if (error) throw error;
}
}