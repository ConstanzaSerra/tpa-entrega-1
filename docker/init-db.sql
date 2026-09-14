-- Un esquema de datos por servicio, como pide la Entrega 3.
-- Se usan dos bases separadas (no dos schemas dentro de una): asi ningun
-- servicio puede alcanzar las tablas del otro ni siquiera por accidente,
-- que es justamente lo que la consigna quiere garantizar.

CREATE DATABASE donatrack_donaciones OWNER donatrack;
CREATE DATABASE donatrack_logistica  OWNER donatrack;
