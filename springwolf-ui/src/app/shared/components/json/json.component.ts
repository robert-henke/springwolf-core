/* SPDX-License-Identifier: Apache-2.0 */
import { Component, OnInit, Input } from "@angular/core";

@Component({
  selector: "app-json",
  template: '<markdown [data]="json"></markdown>',
})
export class JsonComponent implements OnInit {
  @Input() data: any;
  @Input() json: string;

  ngOnInit(): void {
    this.json =
      this.json === undefined
        ? "```json\n" + JSON.stringify(this.data, null, 2) + "\n```"
        : this.json;
  }
}
