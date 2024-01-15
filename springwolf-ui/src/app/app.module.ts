/* SPDX-License-Identifier: Apache-2.0 */
import { HttpClientModule } from "@angular/common/http";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientInMemoryWebApiModule } from "angular-in-memory-web-api";
import { HIGHLIGHT_OPTIONS, HighlightModule } from "ngx-highlightjs";
import { environment } from "./../environments/environment";
import { AppComponent } from "./app.component";
import { ChannelMainComponent } from "./channels/channel-main/channel-main.component";
import { ChannelsComponent } from "./channels/channels.component";
import { HeaderComponent } from "./header/header.component";
import { InfoComponent } from "./info/info.component";
import { MaterialModule } from "./material.module";
import { SchemaComponent } from "./schemas/schema/schema.component";
import { SchemasComponent } from "./schemas/schemas.component";
import { ServersComponent } from "./servers/servers.component";
import { AsyncApiService } from "./shared/asyncapi.service";
import { MockServer } from "./shared/mock/mock-server";
import { PublisherService } from "./shared/publisher.service";
import { FormsModule } from "@angular/forms";
import { JsonComponent } from "./shared/components/json/json.component";
import { AsyncApiMapperService } from "./shared/asyncapi-mapper.service";
import { MarkdownModule, provideMarkdown } from "ngx-markdown";

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    InfoComponent,
    ServersComponent,
    ChannelsComponent,
    ChannelMainComponent,
    SchemasComponent,
    SchemaComponent,
    JsonComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    MaterialModule,
    HighlightModule,
    HttpClientModule,
    FormsModule,
    MarkdownModule.forRoot(),
    MarkdownModule.forChild(),
    environment.production
      ? []
      : HttpClientInMemoryWebApiModule.forRoot(MockServer, { delay: 100 }),
  ],
  providers: [
    AsyncApiService,
    AsyncApiMapperService,
    PublisherService,
    provideMarkdown(),
    {
      provide: HIGHLIGHT_OPTIONS,
      useValue: {
        coreLibraryLoader: () => import("highlight.js/lib/core"),
        languages: {
          json: () => import("highlight.js/lib/languages/json"),
        },
      },
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
