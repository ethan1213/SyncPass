ALTER TABLE talleres ADD COLUMN latitud DOUBLE PRECISION;
ALTER TABLE talleres ADD COLUMN longitud DOUBLE PRECISION;
ALTER TABLE talleres ADD COLUMN radio_metros INTEGER;
ALTER TABLE talleres ADD CONSTRAINT taller_geocerca_completa CHECK (
  (latitud IS NULL AND longitud IS NULL AND radio_metros IS NULL) OR
  (latitud BETWEEN -90 AND 90 AND longitud BETWEEN -180 AND 180 AND radio_metros BETWEEN 10 AND 5000)
);
ALTER TABLE registros ADD COLUMN latitud DOUBLE PRECISION;
ALTER TABLE registros ADD COLUMN longitud DOUBLE PRECISION;
ALTER TABLE registros ADD COLUMN precision_metros REAL;
