import { Injectable } from '@angular/core';

//Variables constantes
export const PATH_DEFAULT_AVATAR = 'https://sgyidsdrwqkqtqimozvm.supabase.co/storage/v1/object/public/imagenes/source-img/default_avatar.png'
export const PRECIO_LITRO_COMBUSTIBLE = 1.751
export const MAP_COORDENADAS_POR_DEFECTO = {LAT: 40.4168, LNG: -3.7038}

@Injectable({
  providedIn: 'root',
})
export class GlobalService {
  //Aqui pueden existir variables globales que cambien en tiempo de ejecucion
}
