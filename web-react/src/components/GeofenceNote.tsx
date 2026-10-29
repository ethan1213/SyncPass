type GeofenceNoteProps = { required: boolean };

export default function GeofenceNote({ required }: GeofenceNoteProps) {
  if (!required) return null;
  return <p className="rounded-md border border-arcilla p-3 text-sm" role="note">
    Este taller exige ubicación GPS reciente dentro del radio configurado.
  </p>;
}
