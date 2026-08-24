import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, LocateResponse } from '../../services/api';

@Component({
  selector: 'app-locate',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './locate.html',
  styleUrl: './locate.css',
})
export class LocateComponent {
  x: number | null = null;
  y: number | null = null;
  z: number | null = null;
  loading = false;
  result: LocateResponse | null = null;
  error: string | null = null;

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  locate() {
    if (this.x === null || this.y === null || this.z === null) return;

    this.loading = true;
    this.error = null;
    this.result = null;

    this.api.locate({ x: this.x, y: this.y, z: this.z }).subscribe({
      next: (res) => {
        this.result = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Locate failed: ' + (err.error?.error || err.message);
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
