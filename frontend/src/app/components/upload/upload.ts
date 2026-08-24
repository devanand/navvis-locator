import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './upload.html',
  styleUrl: './upload.css',
})
export class UploadComponent {
  private static readonly MAX_FILE_SIZE_MB = 10;
  private static readonly MAX_FILE_SIZE_BYTES = UploadComponent.MAX_FILE_SIZE_MB * 1024 * 1024;

  selectedFile: File | null = null;
  uploading = false;
  buildingsCreated: number | null = null;
  error: string | null = null;

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] || null;

    this.error = null;
    this.buildingsCreated = null;

    if (file && file.size > UploadComponent.MAX_FILE_SIZE_BYTES) {
      this.error = `File size (${(file.size / (1024 * 1024)).toFixed(1)}MB) exceeds the ${UploadComponent.MAX_FILE_SIZE_MB}MB limit`;
      this.selectedFile = null;
      input.value = '';
      return;
    }

    this.selectedFile = file;
  }

  upload() {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.error = null;
    this.buildingsCreated = null;

    this.api.uploadBuildings(this.selectedFile).subscribe({
      next: (res) => {
        this.buildingsCreated = res.buildingsCreated;
        this.uploading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Upload failed: ' + (err.error?.error || err.message);
        this.uploading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
