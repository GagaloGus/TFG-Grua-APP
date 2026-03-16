import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Home } from './home/home';
import { AdminPage } from './admin-page/admin-page';
import { AdminDashboard } from './admin-dashboard/admin-dashboard';
import { AdminServicios } from './admin-servicios/admin-servicios';
import { AdminCreateuser } from './admin-createuser/admin-createuser';
import { AdminUsers } from './admin-users/admin-users';

export const routes: Routes = [
    
    {path: "admin", component:AdminPage,
        children:[
            {path: "dashboard", component:AdminDashboard},
            {path: "servicios", component:AdminServicios},
            {path: "createuser", component:AdminCreateuser},
            {path: "users", component:AdminUsers},
            {path: '', redirectTo: 'dashboard', pathMatch: 'full'},
        ]
    },
    {path: "login", component:Login},
    {path: "home", component:Home},

    // Ruta vacia
    {path: '', redirectTo: '/login', pathMatch: 'full'},

    // Ruta incorrecta
    {path: '**', redirectTo: '/login', pathMatch: 'full'}, 
];
