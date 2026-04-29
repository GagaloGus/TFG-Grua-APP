import { Injectable, signal } from '@angular/core';

//Variables constantes
export const PATH_DEFAULT_AVATAR = '/img/default_avatar.png'
export const PRECIO_LITRO_COMBUSTIBLE = 1.751

@Injectable({
  providedIn: 'root',
})
export class GlobalService {
  //Aqui pueden existir variables globales que cambien en tiempo de ejecucion
}
